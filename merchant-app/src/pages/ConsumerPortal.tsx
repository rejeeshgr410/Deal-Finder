import React, { useState, useMemo, useEffect } from 'react';
import { 
  Search, Store, MapPin, Bookmark, Package, Trash2, Sparkles, 
  Calculator, TrendingDown, ChevronRight, Bell, Clock, Filter, 
  CheckCircle, FileText, Check, AlertCircle, ShoppingCart, ShoppingBag
} from 'lucide-react';
import { useCollection } from '../lib/firebase-hooks';
import { useToast } from '../contexts/ToastContext';
import { trackEvent } from '../lib/analytics';
import { normalizeString } from '../lib/matching';

export default function ConsumerPortal() {
  const { showToast } = useToast();
  
  // Real datasets from B2B system
  const { data: allRetailers, loading: loadingRetailers } = useCollection<any>('retailers');
  const { data: allFlyers, loading: loadingFlyers } = useCollection<any>('flyers');
  const { data: allProducts, loading: loadingProducts } = useCollection<any>('products');
  const { data: coupons } = useCollection<any>('coupons');
  const { data: notifications } = useCollection<any>('notifications');

  // Consumer Filter States
  const [selectedEmirate, setSelectedEmirate] = useState('Dubai');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [selectedRetailerFilter, setSelectedRetailerFilter] = useState('All');
  
  // Navigation tabs inside portal
  const [activeTab, setActiveTab] = useState<'retailers' | 'deals' | 'coupons'>('retailers');
  
  // Selected Detail Modal states
  const [activeFlyerId, setActiveFlyerId] = useState<string | null>(null);
  
  // Shopping List (persisted client side in localState)
  const [shoppingList, setShoppingList] = useState<any[]>(() => {
    const saved = localStorage.getItem('df_shopping_list');
    return saved ? JSON.parse(saved) : [];
  });

  // Save Shopping List
  useEffect(() => {
    localStorage.setItem('df_shopping_list', JSON.stringify(shoppingList));
  }, [shoppingList]);

  // Deep Link handler from simulated notifications
  const handleDeepLink = (type: string, targetId: string) => {
    if (type === 'flyer') {
      setActiveFlyerId(targetId);
      setActiveTab('retailers');
      showToast('Opening deep linked Flyer', 'success');
      trackEvent('flyer_view', { source: 'notification', type: 'flyer', targetId });
    } else if (type === 'retailer') {
      setSelectedRetailerFilter(targetId);
      setActiveTab('retailers');
      showToast('Opening Retailer profile', 'success');
      trackEvent('retailer_page_view', { source: 'notification', type: 'retailer', targetId });
    }
  };

  // 1. Filtered Retailers operating in selected Emirate
  const filteredRetailers = useMemo(() => {
    return allRetailers.filter((ret: any) => {
      const isPublished = ret.status === 'Active' || ret.status === 'published' || !ret.status;
      const matchLoc = !ret.emirates || ret.emirates.length === 0 || ret.emirates.includes(selectedEmirate);
      return isPublished && matchLoc;
    });
  }, [allRetailers, selectedEmirate]);

  // 2. Published flyers of filtered retailers
  const filteredFlyers = useMemo(() => {
    const allowedRetailerIds = new Set(filteredRetailers.map(r => r.id));
    return allFlyers.filter((flyer: any) => {
      const isPublished = flyer.status === 'published' || flyer.status === 'live';
      const belongsToFilteredRetailer = allowedRetailerIds.has(flyer.retailerId);
      return isPublished && belongsToFilteredRetailer;
    });
  }, [allFlyers, filteredRetailers]);

  // 3. Published products (Deals) of filtered retailers
  const filteredProducts = useMemo(() => {
    const allowedRetailerIds = new Set(filteredRetailers.map(r => r.id));
    return allProducts.filter((p: any) => {
      const isPublished = p.status === 'published' || p.status === 'live' || !p.status;
      const belongsToFilteredRetailer = allowedRetailerIds.has(p.retailerId);
      const matchCat = selectedCategory === 'All' || p.category === selectedCategory;
      const matchRet = selectedRetailerFilter === 'All' || p.retailerId === selectedRetailerFilter;
      const matchQuery = !searchQuery || 
        p.name?.toLowerCase().includes(searchQuery.toLowerCase()) || 
        p.brand?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        p.category?.toLowerCase().includes(searchQuery.toLowerCase());
      
      return isPublished && belongsToFilteredRetailer && matchCat && matchRet && matchQuery;
    });
  }, [allProducts, filteredRetailers, selectedCategory, selectedRetailerFilter, searchQuery]);

  // 4. Products matching active selected flyer inside detail modal
  const flyerProducts = useMemo(() => {
    if (!activeFlyerId) return [];
    return allProducts.filter((p: any) => p.flyerId === activeFlyerId && (p.status === 'published' || p.status === 'live'));
  }, [allProducts, activeFlyerId]);

  // CATEGORIES List
  const categories = useMemo(() => {
    const set = new Set<string>();
    allProducts.forEach(p => {
      if (p.category) set.add(p.category);
    });
    return ['All', ...Array.from(set)];
  }, [allProducts]);

  // 5. Comparables Grouping (Group by masterProductId OR normalized name overlap)
  const productGroups = useMemo(() => {
    const groups: Record<string, any[]> = {};
    
    filteredProducts.forEach((p: any) => {
      // Key can be masterProductId or canonical fallback
      const groupKey = p.masterProductId || `norm_${normalizeString(p.name).substring(0, 15)}`;
      if (!groups[groupKey]) {
        groups[groupKey] = [];
      }
      groups[groupKey].push(p);
    });

    return Object.values(groups).filter(g => g.length > 0);
  }, [filteredProducts]);

  // 6. Shopping List & Basket Optimizer calculations
  // Computes basket total for each Retailer
  const basketOptimizerBreakdown = useMemo(() => {
    if (shoppingList.length === 0) return [];

    // Map each retailer ID to total Cost and item list match summary
    const stats: Record<string, { retailerName: string, logoUrl: string, total: number, matchesCount: number, matchingItems: any[] }> = {};
    
    // Initialize stats for active filtered retailers
    filteredRetailers.forEach(ret => {
      stats[ret.id] = {
        retailerName: ret.name,
        logoUrl: ret.logoUrl || '',
        total: 0,
        matchesCount: 0,
        matchingItems: []
      };
    });

    shoppingList.forEach(listItem => {
      // Find matches for this item across all retailers
      // Match key is masterProductId or similar description
      const masterId = listItem.masterProductId;
      const normalizedName = normalizeString(listItem.name);

      filteredRetailers.forEach(ret => {
        // Find best match in database for this retailer
        const match = allProducts.find((p: any) => {
          if (p.retailerId !== ret.id) return false;
          if (p.status !== 'published' && p.status !== 'live') return false;
          if (masterId && p.masterProductId === masterId) return true;
          return normalizeString(p.name) === normalizedName;
        });

        if (match) {
          stats[ret.id].total += match.currentPrice;
          stats[ret.id].matchesCount += 1;
          stats[ret.id].matchingItems.push({
            name: match.name,
            price: match.currentPrice,
            retailerId: ret.id
          });
        } else {
          // If a retailer doesn't carry this exact item, add an estimated average replacement fee!
          // This keeps the basket total comparison mathematically coherent
          stats[ret.id].total += listItem.currentPrice || 10; 
        }
      });
    });

    return Object.entries(stats)
      .map(([retailerId, payload]) => ({
        retailerId,
        ...payload
      }))
      .sort((a, b) => {
        // Rank by match rate first, then cheapest price
        if (b.matchesCount !== a.matchesCount) {
          return b.matchesCount - a.matchesCount;
        }
        return a.total - b.total;
      });
  }, [shoppingList, filteredRetailers, allProducts]);

  // Functions
  const handleAddToShoppingList = (product: any) => {
    const isAlreadyAdded = shoppingList.some(item => item.id === product.id);
    if (isAlreadyAdded) {
       setShoppingList(prev => prev.filter(item => item.id !== product.id));
       showToast(`Removed "${product.name}" from shopping list`, 'info');
    } else {
       setShoppingList(prev => [...prev, product]);
       showToast(`Added "${product.name}" to shopping list`, 'success');
       trackEvent('product_click', { productId: product.id, name: product.name, price: product.currentPrice });
    }
  };

  const handleSimulateCouponSave = async (coupon: any) => {
    showToast(`Saved coupon: ${coupon.title}`, 'success');
    await trackEvent('coupon_save', { couponId: coupon.id, title: coupon.title });
  };

  const clearShoppingList = () => {
    setShoppingList([]);
    showToast('Shopping list cleared', 'info');
  };

  const incrementFlyerView = async (flyer: any) => {
    setActiveFlyerId(flyer.id);
    await trackEvent('flyer_view', { flyerId: flyer.id, title: flyer.title, retailerId: flyer.retailerId });
  };

  const activeFlyerDetails = useMemo(() => {
    return allFlyers.find(f => f.id === activeFlyerId);
  }, [allFlyers, activeFlyerId]);

  const activeFlyerRetailer = useMemo(() => {
    if (!activeFlyerDetails) return null;
    return allRetailers.find(r => r.id === activeFlyerDetails.retailerId);
  }, [allRetailers, activeFlyerDetails]);

  return (
    <div className="flex-1 flex flex-col h-full bg-slate-50">
      
      {/* 1. Header with Emirate Location Dropdown */}
      <div className="bg-white border-b border-slate-200 px-6 py-4 flex flex-col md:flex-row md:items-center justify-between gap-4 shadow-sm select-none shrink-0 rounded-2xl mb-6">
        <div>
           <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
             <ShoppingCart className="w-6 h-6 text-indigo-600 animate-pulse" /> Deal Finder UAE <span className="text-xs bg-indigo-600 text-white font-bold uppercase tracking-widest px-2 py-0.5 rounded-full">Consumer Portal (B2C)</span>
           </h2>
           <p className="text-xs text-slate-500 mt-1">Cross-retailer price matching, digital leaflets, and shopping list optimizer.</p>
        </div>
        
        <div className="flex items-center gap-3">
          <span className="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1"><MapPin className="w-4 h-4 text-indigo-600" /> Selected Emirate</span>
          <select 
            value={selectedEmirate} 
            onChange={(e) => setSelectedEmirate(e.target.value)}
            className="px-4 py-2 border border-slate-200 rounded-xl bg-white-50 font-bold text-sm shadow-sm focus:ring-2 focus:ring-indigo-500 outline-none"
          >
             <option value="Dubai">Dubai</option>
             <option value="Abu Dhabi">Abu Dhabi</option>
             <option value="Sharjah">Sharjah</option>
             <option value="Ajman">Ajman</option>
             <option value="Ras Al Khaimah">Ras Al Khaimah</option>
             <option value="Fujairah">Fujairah</option>
             <option value="Umm Al Quwain">Umm Al Quwain</option>
          </select>
        </div>
      </div>

      {/* 2. Main layout divide (Content + Side Shopping Basket Panel) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 flex-1 items-stretch">
         
         {/* Main Explore Panel */}
         <div className="lg:col-span-2 flex flex-col gap-6">
           
           {/* Navigation Tabs */}
           <div className="flex items-center gap-2 border-b border-slate-200">
             <button 
               onClick={() => setActiveTab('retailers')}
               className={`pb-3 px-4 font-bold text-sm tracking-tight border-b-2 transition-all ${activeTab === 'retailers' ? 'border-indigo-600 text-indigo-700' : 'border-transparent text-slate-500 hover:text-slate-900'}`}
             >
               Leaflets & Retailers
             </button>
             <button 
               onClick={() => setActiveTab('deals')}
               className={`pb-3 px-4 font-bold text-sm tracking-tight border-b-2 transition-all ${activeTab === 'deals' ? 'border-indigo-600 text-indigo-700' : 'border-transparent text-slate-500 hover:text-slate-900'}`}
             >
               Compare Brand Prices
             </button>
             <button 
               onClick={() => setActiveTab('coupons')}
               className={`pb-3 px-4 font-bold text-sm tracking-tight border-b-2 transition-all ${activeTab === 'coupons' ? 'border-indigo-600 text-indigo-700' : 'border-transparent text-slate-500 hover:text-slate-900'}`}
             >
               Available Coupons
             </button>
           </div>

           {/* A. RETAILER & FLYER BROWSING PANEL */}
           {activeTab === 'retailers' && (
             <div className="space-y-6">
               
               {/* Retailers horizontal scroll */}
               <div>
                 <h3 className="font-bold text-slate-900 mb-3 text-sm uppercase tracking-wide flex items-center gap-1">
                   <Store className="w-4 h-4 text-indigo-500"/> Participating Retailers
                 </h3>
                 {loadingRetailers ? (
                   <div className="animate-pulse flex gap-4 overflow-x-auto py-2">
                     {[1, 2, 3].map(x => <div key={x} className="min-w-[150px] h-20 bg-white rounded-2xl border border-slate-200"></div>)}
                   </div>
                 ) : filteredRetailers.length === 0 ? (
                   <p className="text-xs text-slate-500 bg-white p-6 rounded-2xl border border-slate-200">No active retailers registered for {selectedEmirate} yet.</p>
                 ) : (
                   <div className="flex gap-4 overflow-x-auto py-1 pr-2 select-none">
                     {filteredRetailers.map(r => (
                       <button 
                         key={r.id}
                         onClick={() => setSelectedRetailerFilter(selectedRetailerFilter === r.id ? 'All' : r.id)}
                         className={`min-w-[150px] p-4 rounded-2xl border text-center transition-all ${selectedRetailerFilter === r.id ? 'border-indigo-600 bg-indigo-50/50 shadow-md ring-1 ring-indigo-500' : 'border-slate-200 bg-white hover:border-slate-400'}`}
                       >
                         <div className="w-10 h-10 bg-slate-100 rounded-full mx-auto mb-2 flex items-center justify-center font-bold text-slate-500 overflow-hidden border border-slate-100">
                           {r.logoUrl ? <img src={r.logoUrl} alt="" className="w-full h-full object-cover" /> : r.name?.substring(0, 2).toUpperCase()}
                         </div>
                         <div className="font-bold text-xs text-slate-900 truncate">{r.name}</div>
                         <div className="text-[10px] text-indigo-600 font-semibold mt-1">{r.emirates?.length || 0} Emirates</div>
                       </button>
                     ))}
                   </div>
                 )}
               </div>

               {/* Flyers / Leaflets Grid */}
               <div>
                  <h3 className="font-bold text-slate-900 mb-3 text-sm uppercase tracking-wide flex items-center gap-1">
                    <FileText className="w-4 h-4 text-indigo-500" /> Active UAE Promotional Flyers
                  </h3>
                  {loadingFlyers ? (
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                       {[1, 2].map(x => <div key={x} className="h-44 bg-white rounded-2xl animate-pulse"></div>)}
                    </div>
                  ) : filteredFlyers.length === 0 ? (
                    <div className="bg-white p-12 text-center rounded-3xl border border-slate-200 flex flex-col items-center justify-center">
                       <FileText className="w-12 h-12 text-slate-200 mb-3" />
                       <h4 className="font-bold text-slate-800 mb-1">No Active Flyers found</h4>
                       <p className="text-xs text-slate-500">There are no published leaflets currently available for the selected emirate.</p>
                    </div>
                  ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                       {filteredFlyers.map(f => {
                         const ret = allRetailers.find(r => r.id === f.retailerId);
                         return (
                           <div 
                             key={f.id}
                             onClick={() => incrementFlyerView(f)}
                             className="bg-white border border-slate-200 hover:border-indigo-500 rounded-2xl overflow-hidden shadow-sm hover:shadow-md cursor-pointer transition-all flex flex-col h-full"
                           >
                             <div className="h-32 bg-slate-100 relative">
                               {f.imageUrl ? (
                                 <img src={f.imageUrl} alt={f.title} className="w-full h-full object-cover" referrerPolicy="no-referrer" />
                               ) : (
                                 <div className="w-full h-full flex items-center justify-center text-slate-300">
                                   <FileText className="w-12 h-12" />
                                 </div>
                               )}
                               <div className="absolute top-3 left-3 bg-white/95 backdrop-blur-sm px-2.5 py-1 rounded-lg text-[10px] font-bold text-indigo-700 shadow-sm flex items-center gap-1.5 border border-slate-100">
                                 <Store className="w-3.5 h-3.5" />
                                 {ret?.name || 'Retailer'}
                               </div>
                             </div>
                             <div className="p-4 flex-1 flex flex-col justify-between">
                               <div>
                                 <h4 className="font-bold text-slate-900 text-sm line-clamp-1">{f.title}</h4>
                                 <p className="text-[10px] text-slate-500 mt-1 uppercase tracking-wide flex items-center gap-1">
                                   <Clock className="w-3 h-3 text-slate-400" />
                                   Valid to: {f.validTo || f.date || 'End of leaflet'}
                                 </p>
                               </div>
                               <button 
                                 className="mt-4 w-full py-2 bg-indigo-50 group-hover:bg-indigo-100 text-indigo-700 font-bold text-xs rounded-xl transition-all border border-indigo-100 uppercase tracking-widest text-center"
                               >
                                 Browse Offers
                               </button>
                             </div>
                           </div>
                         );
                       })}
                    </div>
                  )}
               </div>
             </div>
           )}

           {/* B. SEARCH & BRAND DIRECT COMPARISON TABS */}
           {activeTab === 'deals' && (
             <div className="space-y-6">
               
               {/* Filters Bar */}
               <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-col md:flex-row items-center gap-4">
                 <div className="relative flex-1 w-full">
                   <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                   <input 
                     type="text"
                     value={searchQuery}
                     onChange={(e) => setSearchQuery(e.target.value)}
                     placeholder="Search brand products (e.g., Almarai Milk, Rice...)"
                     className="w-full pl-10 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white"
                   />
                 </div>

                 <div className="flex gap-2 w-full md:w-auto overflow-x-auto py-1">
                   {categories.map(cat => (
                     <button 
                       key={cat}
                       onClick={() => setSelectedCategory(cat)}
                       className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all whitespace-nowrap ${selectedCategory === cat ? 'bg-indigo-600 text-white shadow' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`}
                     >
                       {cat}
                     </button>
                   ))}
                 </div>
               </div>

               {/* Comparison Result Cards */}
               <div className="space-y-4">
                 {loadingProducts ? (
                   <p className="text-center font-medium py-10 animate-pulse text-slate-500">Loading catalog...</p>
                 ) : productGroups.length === 0 ? (
                   <div className="bg-white p-12 text-center rounded-3xl border border-slate-200 flex flex-col items-center justify-center">
                     <Package className="w-12 h-12 text-slate-200 mb-3" />
                     <h4 className="font-bold text-slate-800 mb-1">No products found</h4>
                     <p className="text-xs text-slate-500">Try adjusting your filters, selecting another Emirate, or choosing another keyword.</p>
                   </div>
                 ) : (
                    productGroups.map((group, idx) => {
                      // Grab canonical values from first item or master link
                      const sampleProduct = group[0];
                      const brand = sampleProduct.brand || 'General';
                      const category = sampleProduct.category || 'Grocery';
                      
                      // Sort group items so cheapest price comes first
                      const sortedOffers = [...group].sort((a, b) => a.currentPrice - b.currentPrice);
                      const cheapest = sortedOffers[0];
                      const highest = sortedOffers[sortedOffers.length - 1];
                      const potentialSavings = highest.currentPrice - cheapest.currentPrice;

                      return (
                        <div key={idx} className="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-sm flex flex-col md:flex-row divide-y md:divide-y-0 md:divide-x divide-slate-100">
                          
                          {/* Left Panel: Product Metadata */}
                          <div className="p-5 md:w-1/3 flex flex-col justify-between bg-slate-50/50">
                             <div>
                               <span className="text-[10px] font-extrabold uppercase tracking-widest text-indigo-600 bg-indigo-50 px-2.5 py-1 rounded inline-block mb-3">{category}</span>
                               <h4 className="font-bold text-slate-900 text-base leading-tight">{sampleProduct.name}</h4>
                               <p className="text-xs text-slate-500 mt-1">Brand: <span className="font-semibold">{brand}</span> {sampleProduct.unitSize ? `• ${sampleProduct.unitSize} ${sampleProduct.unitType || ''}` : ''}</p>
                             </div>
                             
                             {group.length > 1 && potentialSavings > 0 && (
                               <div className="mt-4 p-3 bg-green-50 text-green-800 rounded-xl border border-green-100 flex items-center gap-2">
                                  <Sparkles className="w-4 h-4 text-green-600 animate-bounce" />
                                  <div className="text-xs leading-tight">
                                     Save up to <span className="font-bold">{potentialSavings.toFixed(2)} AED</span> at cheapest retailer!
                                  </div>
                               </div>
                             )}
                          </div>

                          {/* Right Panel: Participating stores displaying prices side by side */}
                          <div className="p-5 flex-1 flex flex-col justify-between">
                            <span className="text-[10px] font-extrabold uppercase tracking-widest text-slate-400 block mb-3">Compare Store Offers:</span>
                            
                            <div className="space-y-2.5">
                               {sortedOffers.map((offer: any) => {
                                 const rObj = allRetailers.find(ret => ret.id === offer.retailerId);
                                 const isCheapestInGroup = offer.id === cheapest.id && group.length > 1;
                                 const isWishlisted = shoppingList.some(item => item.id === offer.id);

                                 return (
                                   <div key={offer.id} className={`flex items-center justify-between p-3 border rounded-xl transition-all ${isCheapestInGroup ? 'border-green-300 bg-green-50/20' : 'border-slate-100 hover:border-slate-300'}`}>
                                      <div className="flex items-center gap-3">
                                         <div className="w-8 h-8 rounded-full bg-slate-100 flex items-center justify-center font-bold text-xs text-slate-500 overflow-hidden border">
                                            {rObj?.logoUrl ? <img src={rObj.logoUrl} alt="" className="w-full h-full object-cover" /> : rObj?.name?.substring(0, 2).toUpperCase()}
                                         </div>
                                         <div>
                                            <div className="font-bold text-xs text-slate-900">{rObj?.name || 'Retailer'}</div>
                                            <div className="text-[10px] text-slate-400">Offer Validity: {offer.validTo || 'Leaflet'}</div>
                                         </div>
                                      </div>

                                      <div className="flex items-center gap-4">
                                         <div className="text-right">
                                            <div className="font-black text-slate-900 text-sm">{offer.currentPrice.toFixed(2)} AED</div>
                                            {offer.oldPrice > 0 && (
                                               <div className="text-[10px] line-through text-slate-400 font-semibold">{offer.oldPrice.toFixed(2)} AED</div>
                                            )}
                                         </div>

                                         <div className="flex items-center gap-1.5">
                                            {isCheapestInGroup && (
                                               <span className="hidden sm:inline-block text-[9px] font-black uppercase tracking-wider bg-green-100 text-green-700 px-2 py-0.5 rounded-md">Cheapest</span>
                                            )}

                                            <button 
                                              onClick={() => handleAddToShoppingList(offer)}
                                              className={`p-2 rounded-lg transition-all ${isWishlisted ? 'bg-indigo-600 text-white' : 'bg-slate-50 text-slate-600 hover:bg-slate-100'}`}
                                              title={isWishlisted ? 'Saved' : 'Add to Shopping List'}
                                            >
                                               <Check className="w-3.5 h-3.5" />
                                            </button>
                                         </div>
                                      </div>
                                   </div>
                                 );
                               })}
                            </div>
                          </div>

                        </div>
                      );
                    })
                 )}
               </div>

             </div>
           )}

           {/* C. COUPONS TAB */}
           {activeTab === 'coupons' && (
             <div className="space-y-4">
                <h3 className="font-bold text-slate-900 text-sm uppercase tracking-wide flex items-center gap-1">
                  <Bookmark className="w-4 h-4 text-indigo-500" /> Active Discount Codes & Promo Vouchers
                </h3>
                {coupons.length === 0 ? (
                  <div className="bg-white p-12 text-center rounded-3xl border border-slate-200">
                    <p className="text-sm text-slate-500">No promo coupons published yet.</p>
                  </div>
                ) : (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                     {coupons.map((coupon: any) => {
                       const ret = allRetailers.find(r => r.id === coupon.retailerId);
                       return (
                         <div key={coupon.id} className="bg-white border-2 border-dashed border-slate-200 rounded-2xl p-5 hover:border-indigo-400 transition-colors flex flex-col justify-between">
                            <div>
                               <div className="flex items-center justify-between mb-3">
                                 <div className="flex items-center gap-2">
                                   <div className="w-6 h-6 bg-slate-100 rounded-full flex items-center justify-center font-black text-[10px] text-slate-500 overflow-hidden border">
                                      {ret?.logoUrl ? <img src={ret.logoUrl} alt="" className="w-full h-full object-cover" /> : ret?.name?.substring(0, 2).toUpperCase()}
                                   </div>
                                   <span className="text-[10px] uppercase font-black tracking-wider text-slate-600">{ret?.name || 'All Stores'}</span>
                                 </div>
                                 <span className="text-xs bg-indigo-50 text-indigo-700 font-extrabold px-2.5 py-0.5 rounded-lg">{coupon.discountInfo || 'COUPON'}</span>
                               </div>
                               <h4 className="font-bold text-slate-900 text-sm mb-1">{coupon.title}</h4>
                               <p className="text-xs text-slate-500 mb-3 leading-relaxed">{coupon.description || 'Show code at cashier point for immediate activation.'}</p>
                            </div>
                            
                            <div className="space-y-3 pt-3 border-t border-slate-100">
                               <div className="flex items-center justify-between bg-slate-50 px-3 py-2 rounded-xl border border-slate-100 font-mono text-xs font-bold text-indigo-600">
                                  <span>CODE: {coupon.code || 'COUPON_ACTIVE'}</span>
                                  <span className="text-[10px] text-slate-400 uppercase font-bold">Expires: {coupon.expiry || 'End Of Year'}</span>
                               </div>
                               <button 
                                 onClick={() => handleSimulateCouponSave(coupon)}
                                 className="w-full py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs rounded-xl transition-all shadow-md active:scale-95 uppercase tracking-wider text-center"
                               >
                                 Save Coupon
                               </button>
                            </div>
                         </div>
                       );
                     })}
                  </div>
                )}
             </div>
           )}

         </div>

         {/* Basket Optimizer Side Panel */}
         <div className="lg:col-span-1 flex flex-col gap-6">
            
            {/* Shopping List Basket Panel */}
            <div className="bg-white border border-slate-200 rounded-3xl p-5 shadow-sm space-y-5 flex flex-col max-h-[550px] shrink-0">
               <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                     <ShoppingBag className="w-5 h-5 text-indigo-600" />
                     <h3 className="font-bold text-slate-950">Shopping List</h3>
                  </div>
                  {shoppingList.length > 0 && (
                     <button onClick={clearShoppingList} className="text-xs font-bold text-red-600 hover:underline">Clear</button>
                  )}
               </div>

               {/* Shopping Item Rows */}
               <div className="flex-1 overflow-y-auto space-y-2 pr-1 h-36">
                  {shoppingList.length === 0 ? (
                     <div className="text-center py-8 flex flex-col items-center justify-center">
                        <ShoppingCart className="w-10 h-10 text-slate-200 mb-2" />
                        <p className="text-xs text-slate-400 font-medium">Your shopping list is currently empty. Click brand deals to add items.</p>
                     </div>
                  ) : (
                     shoppingList.map((item: any) => {
                       const rObj = allRetailers.find(ret => ret.id === item.retailerId);
                       return (
                         <div key={item.id} className="flex items-center justify-between p-2.5 bg-slate-50 rounded-xl border border-slate-100 text-xs">
                            <div className="flex-1 min-w-0 pr-2">
                               <div className="font-bold text-slate-900 truncate">{item.name}</div>
                               <div className="text-[10px] text-slate-400 flex items-center gap-1.5 mt-0.5">
                                  <span>{item.brand}</span>
                                  <span>•</span>
                                  <span>{rObj?.name || 'Retailer'}</span>
                               </div>
                            </div>
                            <div className="flex items-center gap-3">
                               <span className="font-black text-slate-900 whitespace-nowrap">{item.currentPrice.toFixed(2)} AED</span>
                               <button 
                                 onClick={() => handleAddToShoppingList(item)}
                                 className="text-slate-400 hover:text-red-600 p-1"
                                 title="Remove"
                               >
                                  <Trash2 className="w-3.5 h-3.5" />
                               </button>
                            </div>
                         </div>
                       );
                     })
                  )}
               </div>
            </div>

            {/* Smart Basket Price Optimizer */}
            <div className="bg-gradient-to-br from-indigo-900 to-slate-950 text-white border border-indigo-800 rounded-3xl p-6 shadow-xl flex flex-col gap-4">
               <div>
                  <h3 className="text-base font-extrabold flex items-center gap-2"><Calculator className="w-5 h-5 text-indigo-300" /> B2C Basket Optimizer</h3>
                  <p className="text-[10.5px] text-indigo-200 mt-1">Algorithmic lookup calculates the cheapest physical store to buy your items in {selectedEmirate}!</p>
               </div>

               {shoppingList.length === 0 ? (
                  <div className="p-6 bg-indigo-950/40 rounded-2xl text-center text-xs text-indigo-300 font-medium border border-indigo-800/30">
                     Add brand products to unlock cross-retailer automated price optimization.
                  </div>
               ) : (
                  <div className="space-y-3 max-h-[300px] overflow-y-auto pr-1">
                     {basketOptimizerBreakdown.map((rStat, passIdx) => (
                       <div key={rStat.retailerId} className={`p-4 rounded-2xl border transition-all ${passIdx === 0 ? 'bg-indigo-600/30 border-green-500/50 shadow-md ring-1 ring-green-400/30' : 'bg-slate-900/60 border-slate-800'}`}>
                          <div className="flex items-center justify-between pb-2 border-b border-white/5">
                             <div className="flex items-center gap-2">
                                <span className={`w-5 h-5 rounded-full flex items-center justify-center font-black text-[9px] text-slate-100 ${passIdx === 0 ? 'bg-green-600' : 'bg-slate-700'}`}>
                                  {passIdx + 1}
                                </span>
                                <span className="font-extrabold text-xs text-slate-100">{rStat.retailerName}</span>
                             </div>
                             
                             {passIdx === 0 && (
                               <span className="text-[8.5px] font-black uppercase tracking-wider bg-green-500 text-white px-2 py-0.5 rounded-md">Cheapest Store</span>
                             )}
                          </div>

                          <div className="pt-2.5 flex items-end justify-between">
                             <div>
                                <p className="text-[10px] text-indigo-300 font-medium">Store matches: <span className="font-bold text-white">{rStat.matchesCount} / {shoppingList.length} items</span></p>
                                <p className="text-[9px] text-slate-400 leading-tight mt-0.5">*(unmatched items matched to general average price)</p>
                             </div>
                             <div className="text-right">
                                <span className="text-base font-black text-slate-100">{rStat.total.toFixed(2)} AED</span>
                             </div>
                          </div>
                       </div>
                     ))}
                  </div>
               )}
            </div>

            {/* Deep Linked Notification Center Panel */}
            <div className="bg-white border border-slate-200 rounded-3xl p-5 shadow-sm space-y-4">
               <div>
                  <h3 className="font-extrabold text-xs text-slate-900 uppercase tracking-widest flex items-center gap-2"><Bell className="w-4 h-4 text-slate-400" /> Simulated Push Notifications</h3>
                  <p className="text-[10.5px] text-slate-500 mt-1">Test real-time deep linking actions received from the admin platform.</p>
               </div>

               <div className="space-y-2 pr-1 max-h-[220px] overflow-y-auto">
                  {notifications.length === 0 ? (
                     <div className="p-4 bg-slate-50 rounded-xl text-center text-xs text-slate-400 font-medium">
                        No active simulated pushes logged.
                     </div>
                  ) : (
                     notifications.slice(0, 4).map((notif: any) => {
                       const linkTargetType = notif.targetType === 'retailer_followers' ? 'retailer' : 'flyer';
                       const linkTargetId = notif.targetRetailerId || notif.targetFlyerId || '';
                       
                       return (
                          <div 
                            key={notif.id}
                            className="p-3 bg-slate-50 rounded-xl border border-slate-100 text-xs flex flex-col justify-between"
                          >
                             <div>
                                <p className="font-bold text-slate-900 leading-tight">{notif.title}</p>
                                <p className="text-[10.5px] text-slate-500 mt-0.5">{notif.message}</p>
                             </div>
                             
                             {linkTargetId && (
                                <button 
                                  onClick={() => handleDeepLink(linkTargetType, linkTargetId)}
                                  className="mt-2.5 self-start text-[10px] font-black uppercase tracking-wider text-indigo-600 hover:text-indigo-800 border-b border-indigo-600 pb-0.5 active:scale-95"
                                >
                                   Open Deep Link &rarr;
                                </button>
                             )}
                          </div>
                       );
                     })
                  )}
               </div>
            </div>

         </div>

      </div>

      {/* 3. Flyer Reader Modal (Detail browse panel) */}
      {activeFlyerId && activeFlyerDetails && (
         <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-in fade-in">
           <div className="bg-white rounded-3xl max-w-5xl w-full h-[90vh] overflow-hidden shadow-2xl flex flex-col" onClick={(e) => e.stopPropagation()}>
              
              {/* Modal Banner Header */}
              <div className="p-6 border-b border-slate-100 flex items-center justify-between bg-slate-50">
                 <div className="flex items-center gap-4">
                    <div className="w-12 h-12 rounded-full bg-slate-200 border flex items-center justify-center font-bold text-lg text-slate-600 overflow-hidden">
                       {activeFlyerRetailer?.logoUrl ? <img src={activeFlyerRetailer.logoUrl} alt="" className="w-full h-full object-cover" /> : activeFlyerRetailer?.name?.substring(0, 2).toUpperCase()}
                    </div>
                    <div>
                       <h3 className="font-black text-slate-950 leading-tight">{activeFlyerDetails.title}</h3>
                       <p className="text-xs text-slate-500 mt-1 flex items-center gap-2">
                          <span>Retailer: <span className="font-semibold text-indigo-600">{activeFlyerRetailer?.name}</span></span>
                          <span>•</span>
                          <span>Validity: {activeFlyerDetails.validTo || activeFlyerDetails.date || 'End Of leaflet'}</span>
                       </p>
                    </div>
                 </div>
                 
                 <button 
                   onClick={() => setActiveFlyerId(null)}
                   className="p-2 bg-slate-200/50 hover:bg-slate-200 rounded-full transition-colors text-slate-600 font-extrabold text-xs"
                 >
                    Close Leaflet
                 </button>
              </div>

              {/* Modal Inside Multi Split Panels */}
              <div className="flex-1 flex flex-col md:flex-row divide-y md:divide-y-0 md:divide-x divide-slate-100 min-h-0 overflow-hidden">
                 
                 {/* Mid-Left image grid */}
                 <div className="md:w-1/2 p-6 overflow-y-auto flex items-center justify-center bg-slate-900 border-none relative">
                    {activeFlyerDetails.imageUrl ? (
                       <img src={activeFlyerDetails.imageUrl} alt={activeFlyerDetails.title} className="max-w-full max-h-full object-contain rounded-xl shadow-lg border border-slate-800" referrerPolicy="no-referrer" />
                    ) : (
                       <FileText className="w-48 h-48 text-slate-800" />
                    )}
                 </div>

                 {/* Mid-Right Extracted Deals List */}
                 <div className="md:w-1/2 p-6 flex flex-col h-full min-h-0 overflow-hidden">
                    <span className="text-[10px] font-extrabold uppercase tracking-widest text-slate-400 block mb-3">Leaflet Extracted Products ({flyerProducts.length}):</span>
                    
                    <div className="flex-1 overflow-y-auto space-y-2 pr-1 min-h-0">
                       {flyerProducts.length === 0 ? (
                          <div className="text-center py-16 flex flex-col items-center justify-center">
                             <AlertCircle className="w-10 h-10 text-slate-300 mb-2" />
                             <p className="text-xs text-slate-500">No active products populated for this flyer yet.</p>
                          </div>
                       ) : (
                          flyerProducts.map((pRecord: any) => {
                             const isWishlisted = shoppingList.some(item => item.id === pRecord.id);
                             return (
                               <div key={pRecord.id} className="p-3 bg-slate-50 border border-slate-100 rounded-xl hover:border-slate-300 transition-all flex items-center justify-between">
                                  <div className="flex-1 min-w-0 pr-2">
                                     <h5 className="font-extrabold text-slate-900 text-xs truncate leading-tight">{pRecord.name}</h5>
                                     <p className="text-[10px] text-slate-400 mt-0.5">Category: {pRecord.category} {pRecord.unitSize ? `| Unit: ${pRecord.unitSize} ${pRecord.unitType || ''}` : ''}</p>
                                  </div>

                                  <div className="flex items-center gap-3 shrink-0">
                                     <span className="font-black text-slate-900 text-xs">{pRecord.currentPrice.toFixed(2)} AED</span>
                                     <button 
                                       onClick={() => handleAddToShoppingList(pRecord)}
                                       className={`p-1.5 rounded-lg transition-all ${isWishlisted ? 'bg-indigo-600 text-white' : 'bg-slate-200 text-slate-700 hover:bg-slate-300'}`}
                                       title={isWishlisted ? 'Saved' : 'Add to Shopping List'}
                                     >
                                        <Check className="w-3.5 h-3.5" />
                                     </button>
                                  </div>
                               </div>
                             );
                          })
                       )}
                    </div>
                 </div>

              </div>

           </div>
         </div>
      )}

    </div>
  );
}
