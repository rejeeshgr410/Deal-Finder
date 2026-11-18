import React, { useState, useMemo } from 'react';
import { Package, Search, Filter, Plus, Edit2, Trash2, ChevronLeft, ChevronRight, Check, Link, Cpu, ArrowRightLeft } from 'lucide-react';
import { useCollection } from '../lib/firebase-hooks';
import { Modal } from '../components/Modal';
import { useAuth } from '../contexts/AuthContext';
import { useToast } from '../contexts/ToastContext';
import { normalizeString, calculateWordOverlap, calculateLevenshteinSimilarity, extractUnit, findBestMasterMatch } from '../lib/matching';
import { trackEvent } from '../lib/analytics';
import { where } from 'firebase/firestore';

export default function Products() {
  const { user, isAdminUser, isRetailerUser } = useAuth();
  const { showToast } = useToast();
  const filterKey = `${user.role || ''}:${user.retailerId || ''}`;
  const { data: allProducts, loading, add, update, remove } = useCollection<any>('products', {
    constraints: isRetailerUser && user.retailerId ? [where('retailerId', '==', user.retailerId)] : [],
    enabled: Boolean(user.role),
    filterKey
  });
  const { data: masterProducts, add: addMaster, update: updateMaster, remove: removeMaster } = useCollection<any>('masterProducts');
  const { data: retailers } = useCollection<any>('retailers', {
    constraints: isRetailerUser ? [where('status', '==', 'published')] : [],
    enabled: Boolean(user.role),
    filterKey
  });
  const [activeTab, setActiveTab] = useState<'offers' | 'master' | 'unmatched'>('offers');
  const [searchQuery, setSearchQuery] = useState('');
  const [isAdding, setIsAdding] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [selectedRetailer, setSelectedRetailer] = useState(isRetailerUser ? user.retailerId : '');
  const [newProduct, setNewProduct] = useState({
    name: '', brand: '', category: 'General', currentPrice: 0, oldPrice: 0, barcode: '', unitSize: '', unitType: '', validFrom: '', validTo: '', status: 'published'
  });

  const [newMaster, setNewMaster] = useState({
    canonicalName: '', brand: '', category: 'General', barcode: '', unitSize: '', unitType: '', aliases: '', imageUrl: ''
  });
  
  const [isAddingMaster, setIsAddingMaster] = useState(false);
  const [isEditingMaster, setIsEditingMaster] = useState(false);
  const [editingMasterId, setEditingMasterId] = useState<string | null>(null);
  const [assigningProduct, setAssigningProduct] = useState<any | null>(null);
  const [isMergeModalOpen, setIsMergeModalOpen] = useState(false);

  // Pagination states
  const [currentPage, setCurrentPage] = useState(1);
  const ITEMS_PER_PAGE = 20;

  // Custom smart-match alias linkage choice
  const [shouldAddAlias, setShouldAddAlias] = useState(true);
  
  // Create master automatically from a product
  const [autoLinkId, setAutoLinkId] = useState<string | null>(null);

  // Merge tool states
  const [mergeTab, setMergeTab] = useState<'offers' | 'masters'>('offers');
  const [mergingSourceId, setMergingSourceId] = useState<string>('');
  const [mergingTargetId, setMergingTargetId] = useState<string>('');
  const [isMergingInProgress, setIsMergingInProgress] = useState(false);

  const products = useMemo(() => {
    if (isAdminUser) return allProducts;
    return allProducts.filter((p: any) => p.retailerId === user.retailerId);
  }, [allProducts, user, isAdminUser]);

  const unmatchedProducts = useMemo(() => {
    return products.filter((p: any) => !p.masterProductId);
  }, [products]);

  const filteredProducts = useMemo(() => {
    let source = products;
    if (activeTab === 'unmatched') source = unmatchedProducts;

    return source.filter((p: any) => {
      const searchTerms = [p.name, p.barcode, p.brand].filter(Boolean).map(s => s.toLowerCase());
      return searchTerms.some(term => term.includes(searchQuery.toLowerCase()));
    });
  }, [products, unmatchedProducts, searchQuery, activeTab]);

  const filteredMaster = useMemo(() => {
    return masterProducts.filter((p: any) => {
      const searchTerms = [p.canonicalName, p.barcode, p.brand, p.aliases].filter(Boolean).map(String).map(s => s.toLowerCase());
      return searchTerms.some(term => term.includes(searchQuery.toLowerCase()));
    });
  }, [masterProducts, searchQuery]);

  // Apply Pagination
  const paginatedProducts = useMemo(() => {
    const startIdx = (currentPage - 1) * ITEMS_PER_PAGE;
    return filteredProducts.slice(startIdx, startIdx + ITEMS_PER_PAGE);
  }, [filteredProducts, currentPage]);

  const paginatedMaster = useMemo(() => {
    const startIdx = (currentPage - 1) * ITEMS_PER_PAGE;
    return filteredMaster.slice(startIdx, startIdx + ITEMS_PER_PAGE);
  }, [filteredMaster, currentPage]);

  // Reset page when tab or search changes
  React.useEffect(() => {
    setCurrentPage(1);
  }, [activeTab, searchQuery]);

  // Debounced search terms analytics logger
  React.useEffect(() => {
    if (!searchQuery.trim()) return;
    const delayDebounceFn = setTimeout(async () => {
      await trackEvent('search', { query: searchQuery.trim(), tab: activeTab });
    }, 1500);
    return () => clearTimeout(delayDebounceFn);
  }, [searchQuery, activeTab]);

  const totalPages = Math.ceil((activeTab === 'master' ? filteredMaster.length : filteredProducts.length) / ITEMS_PER_PAGE);

  // Compute master duplicates list
  const masterDuplicates = useMemo(() => {
    const duplicatesList: { m1: any; m2: any; similarity: number; reason: string }[] = [];
    if (!masterProducts || masterProducts.length < 2) return [];
    
    for (let i = 0; i < masterProducts.length; i++) {
      const m1 = masterProducts[i];
      const m1Normalized = normalizeString(m1.canonicalName);
      
      for (let j = i + 1; j < masterProducts.length; j++) {
        const m2 = masterProducts[j];
        const m2Normalized = normalizeString(m2.canonicalName);
        
        let isDup = false;
        let reason = '';
        let similarity = 0;
        
        // Match 1: Same barcodes (if set)
        if (m1.barcode && m2.barcode && m1.barcode.trim() === m2.barcode.trim()) {
          isDup = true;
          reason = 'Same Barcode';
          similarity = 100;
        }
        
        // Match 2: Name overlap/similarity (very high)
        if (!isDup) {
          const mOverlap = calculateWordOverlap(m1.canonicalName, m2.canonicalName);
          const mLev = calculateLevenshteinSimilarity(m1Normalized, m2Normalized);
          similarity = Math.round(Math.max(mOverlap, mLev) * 100);
          if (similarity >= 80) {
            isDup = true;
            reason = `Fuzzy Similarity (${similarity}%)`;
          }
        }
        
        if (isDup) {
          duplicatesList.push({ m1, m2, similarity, reason });
        }
      }
    }
    return duplicatesList;
  }, [masterProducts]);

  const handleEditClick = (product: any) => {
    setNewProduct({
      name: product.name || '',
      brand: product.brand || '',
      category: product.category || 'General',
      currentPrice: product.currentPrice || 0,
      oldPrice: product.oldPrice || 0,
      barcode: product.barcode || '',
      unitSize: product.unitSize || '',
      unitType: product.unitType || '',
      validFrom: product.validFrom || '',
      validTo: product.validTo || '',
      status: product.status || 'published'
    });
    setSelectedRetailer(product.retailerId || '');
    setEditingId(product.id);
    setIsEditing(true);
  };

  const resetForm = () => {
    setNewProduct({ name: '', brand: '', category: 'General', currentPrice: 0, oldPrice: 0, barcode: '', unitSize: '', unitType: '', validFrom: '', validTo: '', status: 'published' });
    setSelectedRetailer(isRetailerUser ? user.retailerId : '');
    setEditingId(null);
  };

  const handleAddProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const payload = { ...newProduct, retailerId: isRetailerUser ? user.retailerId : selectedRetailer };
      await add(payload);
      setIsAdding(false);
      resetForm();
      showToast('Product added successfully', 'success');
    } catch (err) {
      showToast('Failed to add product', 'error');
    }
  };

  const handleUpdateProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingId) {
        const payload = { ...newProduct, retailerId: isRetailerUser ? user.retailerId : selectedRetailer };
        await update(editingId, payload);
      }
      setIsEditing(false);
      resetForm();
      showToast('Product updated successfully', 'success');
    } catch (err) {
      showToast('Failed to update product', 'error');
    }
  };

  const handleDeleteProduct = async (id: string, name: string) => {
    if (window.confirm(`Are you sure you want to delete product "${name}"?`)) {
      try {
        await remove(id);
        showToast('Product deleted successfully', 'success');
      } catch (err) {
        showToast('Failed to delete product', 'error');
      }
    }
  };

  const handleEditMasterClick = (product: any) => {
    setNewMaster({
      canonicalName: product.canonicalName || '',
      brand: product.brand || '',
      category: product.category || 'General',
      barcode: product.barcode || '',
      unitSize: product.unitSize || '',
      unitType: product.unitType || '',
      aliases: Array.isArray(product.aliases) ? product.aliases.join(', ') : (product.aliases || ''),
      imageUrl: product.imageUrl || ''
    });
    setEditingMasterId(product.id);
    setIsEditingMaster(true);
  };

  const resetMasterForm = () => {
    setNewMaster({ canonicalName: '', brand: '', category: 'General', barcode: '', unitSize: '', unitType: '', aliases: '', imageUrl: '' });
    setEditingMasterId(null);
    setAutoLinkId(null);
  };

  const handleAddMaster = async (e: React.FormEvent) => {
    e.preventDefault();
    const payload = { 
      ...newMaster, 
      aliases: newMaster.aliases.split(',').map(s => s.trim()).filter(Boolean)
    };
    try {
      const docRef = await addMaster(payload);
      if (docRef && autoLinkId) {
        await update(autoLinkId, { masterProductId: docRef.id, status: 'published' });
        showToast('Master product created and offer matched!', 'success');
      } else {
        showToast('Master product created successfully.', 'success');
      }
    } catch {
      showToast('Failed to create master product.', 'error');
    }
    setIsAddingMaster(false);
    resetMasterForm();
  };

  const handleUpdateMaster = async (e: React.FormEvent) => {
    e.preventDefault();
    if (editingMasterId) {
      const payload = { 
        ...newMaster, 
        aliases: newMaster.aliases.split(',').map(s => s.trim()).filter(Boolean)
      };
      await updateMaster(editingMasterId, payload);
    }
    setIsEditingMaster(false);
    resetMasterForm();
  };

  const handleAssignMasterClick = (product: any | any[]) => {
    if (Array.isArray(product)) {
      setAssigningProduct(product);
    } else {
      setAssigningProduct([product]);
    }
  };

  const handleAssignSelect = async (masterId: string) => {
    if (assigningProduct && assigningProduct.length > 0) {
      for (const p of assigningProduct) {
         await update(p.id, { masterProductId: masterId });
         
         if (shouldAddAlias) {
           const targetObj = masterProducts.find((m: any) => m.id === masterId);
           if (targetObj) {
             const extAliases = targetObj.aliases || [];
             if (!extAliases.some((a: string) => a.toLowerCase() === p.name.toLowerCase())) {
               await updateMaster(masterId, { aliases: [...extAliases, p.name] });
             }
           }
         }
      }
      setAssigningProduct(null);
      showToast('Offer successfully matched to master product', 'success');
    }
  };

  const handleAutoCreateMaster = (product: any) => {
    const unitInfo = extractUnit(product.name);
    setNewMaster({
      canonicalName: product.name,
      brand: product.brand && product.brand !== 'Extracted' ? product.brand : '',
      category: product.category || 'General',
      barcode: product.barcode || '',
      unitSize: product.unitSize || (unitInfo.size ? String(unitInfo.size) : ''),
      unitType: product.unitType || (unitInfo.type || ''),
      aliases: product.name,
      imageUrl: product.imageUrl || ''
    });
    setAutoLinkId(product.id);
    setIsAddingMaster(true);
  };

  const handleApproveUnmatchedDirect = async (productId: string) => {
    try {
      await update(productId, { status: 'published' });
      showToast('Offer directly approved and published!', 'success');
    } catch {
      showToast('Failed to approve offer.', 'error');
    }
  };

  const handleMergeMasters = async () => {
    if (!mergingSourceId || !mergingTargetId) {
      showToast('Please select both source and target products.', 'error');
      return;
    }
    if (mergingSourceId === mergingTargetId) {
      showToast('Source and target master products must be different.', 'error');
      return;
    }

    setIsMergingInProgress(true);
    try {
      const sourceMaster = masterProducts.find((m: any) => m.id === mergingSourceId);
      const targetMaster = masterProducts.find((m: any) => m.id === mergingTargetId);

      if (!sourceMaster || !targetMaster) {
        showToast('Invalid source or target master product selected.', 'error');
        setIsMergingInProgress(false);
        return;
      }

      // Combine aliases array
      const combinedAliases = Array.from(new Set([
        ...(targetMaster.aliases || []),
        sourceMaster.canonicalName,
        ...(sourceMaster.aliases || [])
      ])).filter(Boolean);

      const updatedFields: any = {
        aliases: combinedAliases,
        barcode: targetMaster.barcode || sourceMaster.barcode || '',
        brand: targetMaster.brand || sourceMaster.brand || '',
        category: targetMaster.category || sourceMaster.category || 'General',
        unitSize: targetMaster.unitSize || sourceMaster.unitSize || '',
        unitType: targetMaster.unitType || sourceMaster.unitType || '',
        imageUrl: targetMaster.imageUrl || sourceMaster.imageUrl || ''
      };

      await updateMaster(targetMaster.id, updatedFields);

      // Point affected retailer offers to the surviving target ID
      const affectedOffers = allProducts.filter((p: any) => p.masterProductId === sourceMaster.id);
      for (const offer of affectedOffers) {
        await update(offer.id, { masterProductId: targetMaster.id });
      }

      // Delete source
      await removeMaster(sourceMaster.id);

      showToast(`Merged "${sourceMaster.canonicalName}" into "${targetMaster.canonicalName}" successfully!`, 'success');
      setMergingSourceId('');
      setMergingTargetId('');
    } catch (err) {
      console.error(err);
      showToast('Failed to merge products.', 'error');
    } finally {
      setIsMergingInProgress(false);
    }
  };

  const getRetailerName = (id: string) => {
    const r = retailers.find((ret: any) => ret.id === id);
    return r ? r.name : id;
  };

  return (
    <div className="flex-1 flex flex-col h-full">
      <div className="mb-6 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">{isAdminUser ? 'Products Catalog' : 'My Products'}</h1>
          <p className="text-slate-500 mt-1 text-sm">Manage all extracted and imported products.</p>
        </div>
        <div className="flex items-center gap-3 w-full sm:w-auto">
          {isAdminUser && activeTab === 'master' && (
            <button 
              onClick={() => setIsMergeModalOpen(true)}
              className="flex items-center gap-2 px-4 py-2 border border-slate-200 text-slate-700 bg-white rounded-xl hover:bg-slate-50 transition-colors font-semibold text-sm justify-center"
            >
              <Search className="w-4 h-4" />
              Find Duplicates
            </button>
          )}
          {activeTab === 'master' ? (
            <button onClick={() => setIsAddingMaster(true)} className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors font-semibold text-sm justify-center">
              <Plus className="w-4 h-4" />
              Add Master Product
            </button>
          ) : (
            <button onClick={() => setIsAdding(true)} className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors font-semibold text-sm justify-center">
              <Plus className="w-4 h-4" />
              Add Product
            </button>
          )}
        </div>
      </div>

      {isAdminUser && (
        <div className="flex items-center gap-1 bg-slate-100 p-1 rounded-xl mb-6 self-start">
          <button 
            onClick={() => setActiveTab('offers')} 
            className={`px-4 py-2 rounded-lg text-sm font-bold transition-all ${activeTab === 'offers' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
          >
            All Retailer Offers
          </button>
          <button 
            onClick={() => setActiveTab('master')} 
            className={`px-4 py-2 rounded-lg text-sm font-bold transition-all ${activeTab === 'master' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
          >
            Master Catalog
          </button>
          <button 
            onClick={() => setActiveTab('unmatched')} 
            className={`px-4 py-2 rounded-lg text-sm font-bold transition-all ${activeTab === 'unmatched' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
          >
            Unmatched OCR Products ({unmatchedProducts.length})
          </button>
        </div>
      )}

      <div className="bg-white border border-slate-200 rounded-3xl shadow-sm overflow-hidden flex-1 flex flex-col">
        <div className="p-4 border-b border-slate-100 flex flex-col sm:flex-row gap-4 items-center justify-between shrink-0">
            <div className="relative flex-1 w-full max-w-md">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 w-4 h-4" />
              <input 
                type="text" 
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search products by title, brand, or barcode..." 
                className="w-full pl-9 pr-4 py-2 bg-slate-50 border-none rounded-full focus:ring-2 focus:ring-indigo-500 text-sm outline-none transition-all"
              />
            </div>
            <button className="flex items-center gap-2 px-4 py-2 border border-slate-200 text-slate-700 bg-white rounded-xl hover:bg-slate-50 transition-colors font-semibold text-sm w-full sm:w-auto justify-center">
              <Filter className="w-4 h-4" />
              Filters
            </button>
        </div>

        <div className="overflow-x-auto flex-1">
          {loading ? (
             <div className="p-12 text-center text-slate-500 font-medium animate-pulse">Loading products...</div>
          ) : activeTab === 'master' ? (
             filteredMaster.length === 0 ? (
               <div className="p-12 text-center flex flex-col items-center justify-center h-full">
                 <Package className="w-12 h-12 text-slate-300 mb-4" />
                 <h3 className="text-lg font-bold text-slate-900 mb-1">No master products found</h3>
                 <p className="text-sm text-slate-500">Add a product to the master catalog to start mapping.</p>
               </div>
             ) : (
               <table className="w-full text-left border-collapse">
                 <thead>
                   <tr className="bg-slate-50/50 border-b border-slate-200">
                     <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">Canonical Name</th>
                     <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">Barcode</th>
                     <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">Category</th>
                     <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider text-right">Actions</th>
                   </tr>
                 </thead>
                 <tbody className="divide-y divide-slate-100">
                   {paginatedMaster.map((product: any) => (
                     <tr key={product.id} className="hover:bg-slate-50 transition-colors">
                       <td className="py-4 px-6">
                         <div className="flex items-center gap-3">
                           <div className="w-10 h-10 bg-slate-100 rounded-xl flex items-center justify-center text-slate-400 overflow-hidden border border-slate-100 shrink-0">
                           {product.imageUrl ? (
                             <img src={product.imageUrl} alt="" className="w-full h-full object-cover rounded-xl" referrerPolicy="no-referrer" />
                           ) : (
                             <Package className="w-5 h-5" />
                           )}
                         </div>
                         <div className="hidden">
                             <Package className="w-5 h-5" />
                           </div>
                           <div>
                             <div className="font-semibold text-slate-900">{product.canonicalName}</div>
                              {product.aliases && product.aliases.length > 0 && (
                                <div className="text-[10px] text-slate-400 font-medium max-w-sm truncate animate-fade-in" title={product.aliases.join(', ')}>
                                  Aliases: {product.aliases.join(', ')}
                                </div>
                              )}
                             <div className="text-[11px] text-slate-500 font-medium">Brand: {product.brand || 'Unknown'}</div>
                           </div>
                         </div>
                       </td>
                       <td className="py-4 px-6 text-sm text-slate-600 font-mono text-[11px] font-medium">{product.barcode || '-'}</td>
                       <td className="py-4 px-6 text-sm text-slate-600">
                         <span className="inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-bold uppercase tracking-widest bg-slate-100 text-slate-700">
                           {product.category}
                         </span>
                       </td>
                       <td className="py-4 px-6 text-right">
                         <div className="flex items-center justify-end gap-2">
                            <button onClick={() => handleEditMasterClick(product)} className="text-slate-400 hover:text-indigo-600 p-1.5 rounded-lg hover:bg-indigo-50 transition-colors" title="Edit">
                              <Edit2 className="w-4 h-4" />
                            </button>
                            <button onClick={() => removeMaster(product.id)} className="text-slate-400 hover:text-red-600 p-1.5 rounded-lg hover:bg-red-50 transition-colors" title="Delete">
                              <Trash2 className="w-4 h-4" />
                            </button>
                         </div>
                       </td>
                     </tr>
                   ))}
                 </tbody>
               </table>
             )
          ) : filteredProducts.length === 0 ? (
             <div className="p-12 text-center flex flex-col items-center justify-center h-full">
               <Package className="w-12 h-12 text-slate-300 mb-4" />
               <h3 className="text-lg font-bold text-slate-900 mb-1">No products found</h3>
               <p className="text-sm text-slate-500">Products extracted from flyers will appear here.</p>
             </div>
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50/50 border-b border-slate-200">
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">Product Name</th>
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">Barcode</th>
                  {isAdminUser && <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">Retailer</th>}
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">Category</th>
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">Offer Price</th>
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {paginatedProducts.map((product: any) => (
                  <tr key={product.id} className="hover:bg-slate-50 cursor-pointer transition-colors" onClick={async () => {
                    await trackEvent('product_click', { productId: product.id, name: product.name, retailerId: product.retailerId || '' });
                    showToast(`Product click logged: ${product.name}`, 'info');
                  }}>
                    <td className="py-4 px-6">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 bg-slate-100 rounded-xl flex items-center justify-center text-slate-400 overflow-hidden border border-slate-100 shrink-0">{product.imageUrl ? <img src={product.imageUrl} alt="" className="w-full h-full object-cover rounded-xl" referrerPolicy="no-referrer" /> : <Package className="w-5 h-5" />}</div><div className="hidden">
                          <Package className="w-5 h-5" />
                        </div>
                        <div>
                          <div className="font-semibold text-slate-900">{product.name}</div>
                          <div className="flex items-center gap-2">
                             <div className="text-[11px] text-slate-500 font-medium">Brand: {product.brand || 'Unknown'}</div>
                             {product.masterProductId ? (
                               <span className="text-[10px] bg-green-100 text-green-700 px-1.5 py-0.5 rounded font-bold uppercase tracking-wider">Matched</span>
                             ) : (
                               <span className="text-[10px] bg-orange-100 text-orange-700 px-1.5 py-0.5 rounded font-bold uppercase tracking-wider">Unmatched</span>
                             )}
                          </div>
                        </div>
                      </div>
                    </td>
                    <td className="py-4 px-6 text-sm text-slate-600 font-mono text-[11px] font-medium">{product.barcode || '-'}</td>
                    {isAdminUser && <td className="py-4 px-6 text-sm text-slate-600 font-medium">{getRetailerName(product.retailerId) || '-'}</td>}
                    <td className="py-4 px-6 text-sm text-slate-600">
                      <span className="inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-bold uppercase tracking-widest bg-slate-100 text-slate-700">
                        {product.category}
                      </span>
                    </td>
                    <td className="py-4 px-6 text-sm font-bold text-green-600">{product.currentPrice ? `AED ${product.currentPrice}` : '-'}</td>
                    <td className="py-4 px-6 text-right">
                      <div className="flex items-center justify-end gap-2">
                         {isAdminUser && !product.masterProductId && (
                            <>
                              <button 
                                onClick={() => handleAssignMasterClick(product)} 
                                className="px-2.5 py-1 text-[10px] bg-slate-100 hover:bg-indigo-50 hover:text-indigo-600 text-slate-705 font-bold uppercase transition-colors flex items-center gap-1"
                                title="Map to existing master product"
                              >
                                <Link className="w-3 h-3" />
                                Map Master
                              </button>
                              <button 
                                onClick={() => handleAutoCreateMaster(product)} 
                                className="px-2.5 py-1 text-[10px] bg-indigo-50 hover:bg-indigo-100 text-indigo-700 rounded-lg transition-colors uppercase font-bold flex items-center gap-1 ml-1"
                                title="Create new master product from this"
                              >
                                <Cpu className="w-3 h-3" />
                                Add Catalog
                              </button>
                              <button 
                                onClick={() => handleApproveUnmatchedDirect(product.id)} 
                                className="px-2.5 py-1 text-[10px] bg-green-50 hover:bg-green-100 text-green-700 rounded-lg transition-colors uppercase font-bold flex items-center gap-1 ml-1"
                                title="Approve Offer unmatched directly"
                              >
                                <Check className="w-3 h-3" />
                                Approve Direct
                              </button>
                            
                            <button onClick={() => handleAssignMasterClick(product)} className="text-slate-400 hover:text-indigo-600 p-1.5 rounded-lg hover:bg-indigo-50 transition-colors text-[10px] uppercase font-bold hidden" title="Assign Master">
                              Assign
                            </button>
                            </>
                         )}
                         <button onClick={() => handleEditClick(product)} className="text-slate-400 hover:text-indigo-600 p-1.5 rounded-lg hover:bg-indigo-50 transition-colors" title="Edit">
                           <Edit2 className="w-4 h-4" />
                         </button>
                         <button onClick={() => handleDeleteProduct(product.id, product.name)} className="text-slate-400 hover:text-red-600 p-1.5 rounded-lg hover:bg-red-50 transition-colors" title="Delete">
                           <Trash2 className="w-4 h-4" />
                         </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
        
        {totalPages > 1 && (
          <div className="px-6 py-4 border-t border-slate-200 bg-slate-50/50 flex justify-between items-center shrink-0">
            <span className="text-xs text-slate-500 font-medium">
              Showing {(currentPage - 1) * ITEMS_PER_PAGE + 1} - {Math.min(currentPage * ITEMS_PER_PAGE, activeTab === 'master' ? filteredMaster.length : filteredProducts.length)} of {activeTab === 'master' ? filteredMaster.length : filteredProducts.length}
            </span>
            <div className="flex items-center gap-2">
              <button 
                disabled={currentPage === 1}
                onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                className="p-1 border border-slate-200 bg-white rounded-lg text-slate-600 hover:bg-slate-50 disabled:opacity-50 transition-colors"
                title="Previous Page"
              >
                <ChevronLeft className="w-5 h-5" />
              </button>
              <div className="text-xs font-bold text-slate-700 px-2">{currentPage} / {totalPages}</div>
              <button 
                disabled={currentPage === totalPages}
                onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
                className="p-1 border border-slate-200 bg-white rounded-lg text-slate-600 hover:bg-slate-50 disabled:opacity-50 transition-colors"
                title="Next Page"
              >
                <ChevronRight className="w-5 h-5" />
              </button>
            </div>
          </div>
        )}
      </div>

      <Modal isOpen={isAdding || isEditing} onClose={() => { setIsAdding(false); setIsEditing(false); resetForm(); }} title={isEditing ? "Edit Product" : "Add Product"}>
        <form onSubmit={isEditing ? handleUpdateProduct : handleAddProduct} className="space-y-4">
          <div>
            <label className="block text-sm font-semibold text-slate-900 mb-1">Product Name</label>
            <input 
              type="text" 
              required
              value={newProduct.name}
              onChange={(e) => setNewProduct({...newProduct, name: e.target.value})}
              className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors"
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-semibold text-slate-900 mb-1">Brand</label>
              <input 
                type="text" 
                value={newProduct.brand}
                onChange={(e) => setNewProduct({...newProduct, brand: e.target.value})}
                className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors"
              />
            </div>
            <div>
              <label className="block text-sm font-semibold text-slate-900 mb-1">Category</label>
              <input 
                type="text" 
                value={newProduct.category}
                onChange={(e) => setNewProduct({...newProduct, category: e.target.value})}
                className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-semibold text-slate-900 mb-1">Offer Price (AED)</label>
              <input 
                type="number" 
                step="0.01"
                placeholder="25.00"
                value={newProduct.currentPrice}
                onChange={(e) => setNewProduct({...newProduct, currentPrice: parseFloat(e.target.value) || 0})}
                className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors"
              />
            </div>
            <div>
              <label className="block text-sm font-semibold text-slate-900 mb-1">Old Price (AED)</label>
              <input 
                type="number"
                step="0.01"
                placeholder="35.00"
                value={newProduct.oldPrice}
                onChange={(e) => setNewProduct({...newProduct, oldPrice: parseFloat(e.target.value) || 0})}
                className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            {isAdminUser && (
              <div>
                <label className="block text-sm font-semibold text-slate-900 mb-1">Retailer</label>
                <select 
                  required
                  value={selectedRetailer}
                  onChange={(e) => setSelectedRetailer(e.target.value)}
                  className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors"
                >
                  <option value="">Select a retailer</option>
                  {retailers.map((r: any) => <option key={r.id} value={r.id}>{r.name}</option>)}
                </select>
              </div>
            )}
            <div>
              <label className="block text-sm font-semibold text-slate-900 mb-1">Barcode (EAN/UPC)</label>
              <input 
                type="text" 
                value={newProduct.barcode}
                onChange={(e) => setNewProduct({...newProduct, barcode: e.target.value})}
                className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors"
                placeholder="Scan or type barcode"
              />
            </div>
          </div>
          <div className="pt-4 flex items-center justify-end gap-3">
            <button 
              type="button" 
              onClick={() => { setIsAdding(false); setIsEditing(false); resetForm(); }}
              className="px-4 py-2 text-sm font-bold text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
            >
              Cancel
            </button>
            <button 
              type="submit" 
              className="px-6 py-2 text-sm font-bold bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors"
            >
              {isEditing ? "Save Changes" : "Save Product"}
            </button>
          </div>
        </form>
      </Modal>
      <Modal isOpen={isAddingMaster || isEditingMaster} onClose={() => { setIsAddingMaster(false); setIsEditingMaster(false); resetMasterForm(); }} title={isEditingMaster ? "Edit Master Product" : "Add Master Product"}>
        <form onSubmit={isEditingMaster ? handleUpdateMaster : handleAddMaster} className="space-y-4">
          <div>
             <label className="block text-sm font-semibold text-slate-900 mb-1">Canonical Name</label>
             <input type="text" required value={newMaster.canonicalName} onChange={(e) => setNewMaster({...newMaster, canonicalName: e.target.value})} className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors" />
          </div>
          <div className="grid grid-cols-2 gap-4">
             <div>
               <label className="block text-sm font-semibold text-slate-900 mb-1">Brand</label>
               <input type="text" value={newMaster.brand} onChange={(e) => setNewMaster({...newMaster, brand: e.target.value})} className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors" />
             </div>
             <div>
               <label className="block text-sm font-semibold text-slate-900 mb-1">Category</label>
               <input type="text" value={newMaster.category} onChange={(e) => setNewMaster({...newMaster, category: e.target.value})} className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors" />
             </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
             <div>
               <label className="block text-sm font-semibold text-slate-900 mb-1">Barcode</label>
               <input type="text" value={newMaster.barcode} onChange={(e) => setNewMaster({...newMaster, barcode: e.target.value})} className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors" />
             </div>
             <div>
               <label className="block text-sm font-semibold text-slate-900 mb-1">Unit Size (e.g., 500, 1)</label>
               <input type="text" value={newMaster.unitSize} onChange={(e) => setNewMaster({...newMaster, unitSize: e.target.value})} className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors" />
             </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
             <div>
               <label className="block text-sm font-semibold text-slate-900 mb-1">Unit Type (e.g., g, ml)</label>
               <input type="text" value={newMaster.unitType} onChange={(e) => setNewMaster({...newMaster, unitType: e.target.value})} className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors" />
             </div>
             <div>
               <label className="block text-sm font-semibold text-slate-900 mb-1">Aliases (comma separated)</label>
               <input type="text" value={newMaster.aliases} onChange={(e) => setNewMaster({...newMaster, aliases: e.target.value})} className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors" placeholder="Coke, Coca-Cola" />
             </div>
          </div>
          <div className="pt-4 flex items-center justify-end gap-3">
            <button type="button" onClick={() => { setIsAddingMaster(false); setIsEditingMaster(false); resetMasterForm(); }} className="px-4 py-2 text-sm font-bold text-slate-600 hover:bg-slate-100 rounded-lg transition-colors">Cancel</button>
            <button type="submit" className="px-6 py-2 text-sm font-bold bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors">{isEditingMaster ? "Save Changes" : "Save Master Product"}</button>
          </div>
        </form>
      </Modal>

      <Modal isOpen={!!assigningProduct} onClose={() => setAssigningProduct(null)} title="Assign to Master Product">
        <div className="space-y-4">
          <p className="text-sm text-slate-600 mb-4">
             Map <strong>{assigningProduct?.length > 1 ? `${assigningProduct.length} products` : assigningProduct?.[0]?.name}</strong> to a canonical master product.
          </p>
          <div className="max-h-60 overflow-y-auto border border-slate-200 rounded-xl divide-y divide-slate-100">
             {masterProducts.map((m: any) => (
                <div key={m.id} className="p-3 hover:bg-slate-50 flex items-center justify-between">
                   <div>
                      <div className="font-semibold text-sm text-slate-900">{m.canonicalName}</div>
                      <div className="text-xs text-slate-500">{m.brand} • {m.barcode || 'No barcode'}</div>
                   </div>
                   <button onClick={() => handleAssignSelect(m.id)} className="text-xs font-bold bg-indigo-50 text-indigo-700 px-3 py-1.5 rounded-lg hover:bg-indigo-100 transition-colors">Select</button>
                </div>
             ))}
             {masterProducts.length === 0 && <div className="p-4 text-center text-sm text-slate-500">No master products available.</div>}
          </div>
          <div className="pt-4 flex items-center justify-end">
            <button onClick={() => setAssigningProduct(null)} className="px-4 py-2 text-sm font-bold text-slate-600 hover:bg-slate-100 rounded-lg transition-colors">Cancel</button>
          </div>
        </div>
      </Modal>

      <Modal isOpen={isMergeModalOpen} onClose={() => setIsMergeModalOpen(false)} title="Duplicate Analysis & Merging Tool">
        <div className="p-4 space-y-4 max-h-[70vh] overflow-y-auto">
          <div className="flex border-b border-slate-200 mb-2">
            <button
              onClick={() => setMergeTab('offers')}
              className={`pb-2 px-3 text-xs font-bold uppercase tracking-wider border-b-2 transition-all ${
                mergeTab === 'offers'
                  ? 'border-indigo-600 text-indigo-600'
                  : 'border-transparent text-slate-400 hover:text-slate-700'
              }`}
            >
              Unmatched Offers Duplicates
            </button>
            <button
              onClick={() => setMergeTab('masters')}
              className={`pb-2 px-3 text-xs font-bold uppercase tracking-wider border-b-2 transition-all ${
                mergeTab === 'masters'
                  ? 'border-indigo-600 text-indigo-600'
                  : 'border-transparent text-slate-400 hover:text-slate-700'
              }`}
            >
              Master Catalog Duplicates
            </button>
          </div>

          {mergeTab === 'offers' ? (
            <>
              <p className="text-xs text-slate-500 mt-1 mb-2">Duplicate finding logic groups identical product names or barcodes from unmatched Retailer Offers to speed up bulk master mapping.</p>
          
          {Object.entries(
             products
               .filter((p: any) => !p.masterProductId)
               .reduce((acc: any, p: any) => {
                 const key = (p.barcode || p.name).toLowerCase();
                 acc[key] = acc[key] ? [...acc[key], p] : [p];
                 return acc;
               }, {})
          )
          .filter(([, group]: any) => group.length > 1)
          .map(([key, group]: any, index) => (
             <div key={index} className="border border-slate-200 rounded-xl p-4 bg-slate-50">
               <h4 className="font-bold text-slate-800 text-sm mb-2 uppercase tracking-wide">Possible Duplicate Match: {key}</h4>
               <div className="space-y-2 mb-3">
                 {group.map((g: any) => (
                   <div key={g.id} className="text-xs flex items-center justify-between bg-white p-2 border border-slate-100 rounded-lg">
                      <div className="flex flex-col">
                         <span className="font-semibold text-slate-900">{g.name}</span>
                         <span className="text-slate-500">{getRetailerName(g.retailerId)} • {g.barcode || 'No barcode'}</span>
                      </div>
                      <span className="font-bold text-green-600">AED {g.currentPrice}</span>
                   </div>
                 ))}
               </div>
               <button onClick={() => { handleAssignMasterClick(group); setIsMergeModalOpen(false); }} className="w-full py-2 bg-indigo-50 text-indigo-700 font-bold text-xs rounded-lg hover:bg-indigo-100">
                  Assign All to Master
               </button>
             </div>
          ))}

          {Object.entries(
             products
               .filter((p: any) => !p.masterProductId)
               .reduce((acc: any, p: any) => {
                 const key = (p.barcode || p.name).toLowerCase();
                 acc[key] = acc[key] ? [...acc[key], p] : [p];
                 return acc;
               }, {})
          ).filter(([, group]: any) => group.length > 1).length === 0 && (
             <div className="text-center py-6 text-slate-400 font-medium">No duplicates found in unmatched products.</div>
           )}
            </>
          ) : (
            <div className="space-y-4">
              <div className="p-4 bg-indigo-50/50 rounded-xl border border-indigo-100 space-y-3">
                <h4 className="text-xs font-bold text-slate-900 uppercase tracking-widest flex items-center gap-1.5">
                  <ArrowRightLeft className="w-3.5 h-3.5 text-indigo-600" />
                  Manual Master Catalog Merge Tool
                </h4>
                <p className="text-[11px] text-slate-600">
                  Select two master products to merge them. The Source product will be deleted, and all of its aliases and retailer product offers will migrate to the Surviving Target product automatically.
                </p>
                <div className="grid grid-cols-2 gap-3 pt-1">
                  <div>
                    <label className="block text-[10px] font-bold uppercase text-slate-500 mb-1">Source (Will be Deleted)</label>
                    <select
                      value={mergingSourceId}
                      onChange={(e) => setMergingSourceId(e.target.value)}
                      className="w-full text-xs font-medium px-2 py-1.5 border border-slate-200 rounded-lg bg-white"
                    >
                      <option value="">-- Choose Source Product --</option>
                      {masterProducts.map((m: any) => (
                        <option key={m.id} value={m.id}>
                          {m.canonicalName} ({m.brand || 'No brand'}) {m.barcode ? `• ${m.barcode}` : ''}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold uppercase text-slate-500 mb-1">Target (Surviving Canonical)</label>
                    <select
                      value={mergingTargetId}
                      onChange={(e) => setMergingTargetId(e.target.value)}
                      className="w-full text-xs font-medium px-2 py-1.5 border border-slate-200 rounded-lg bg-white"
                    >
                      <option value="">-- Choose Target Product --</option>
                      {masterProducts.map((m: any) => (
                        <option key={m.id} value={m.id}>
                          {m.canonicalName} ({m.brand || 'No brand'}) {m.barcode ? `• ${m.barcode}` : ''}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
                <button
                  type="button"
                  disabled={isMergingInProgress || !mergingSourceId || !mergingTargetId}
                  onClick={handleMergeMasters}
                  className="w-full py-2 bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50 font-bold text-xs rounded-lg transition-all"
                >
                  {isMergingInProgress ? 'Merging Catalog Records...' : 'Execute Relational Catalog Merge'}
                </button>
              </div>

              <div className="space-y-2">
                <h4 className="text-xs font-bold text-slate-900 uppercase tracking-widest flex items-center gap-1.5">
                  Fuzzy Duplicates Identified in Master Catalog ({masterDuplicates.length})
                </h4>
                
                <div className="space-y-2 max-h-60 overflow-y-auto pr-1">
                  {masterDuplicates.map((dup: any, index: number) => (
                    <div key={index} className="p-3 bg-slate-50 rounded-xl border border-slate-200 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2.5">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-[10px] font-extrabold uppercase tracking-widest bg-red-100 text-red-800 px-1.5 py-0.5 rounded">
                            {dup.similarity}% match
                          </span>
                          <span className="text-[10px] text-slate-500 font-medium">({dup.reason})</span>
                        </div>
                        <div className="text-xs font-semibold text-slate-900 mt-1">
                          {dup.m1.canonicalName} <span className="text-slate-400 font-normal">vs</span> {dup.m2.canonicalName}
                        </div>
                        <div className="text-[10px] text-slate-400 font-medium mt-0.5">
                          Barcodes: {dup.m1.barcode || 'None'} / {dup.m2.barcode || 'None'}
                        </div>
                      </div>
                      <button
                        type="button"
                        onClick={async () => {
                          if (window.confirm(`Are you sure you want to merge "${dup.m2.canonicalName}" into "${dup.m1.canonicalName}"?`)) {
                            const sourceMaster = dup.m2;
                            const targetMaster = dup.m1;
                            try {
                              const combinedAliases = Array.from(new Set([
                                ...(targetMaster.aliases || []),
                                sourceMaster.canonicalName,
                                ...(sourceMaster.aliases || [])
                              ])).filter(Boolean);
                              
                              await updateMaster(targetMaster.id, {
                                aliases: combinedAliases,
                                barcode: targetMaster.barcode || sourceMaster.barcode || '',
                                brand: targetMaster.brand || sourceMaster.brand || '',
                                category: targetMaster.category || sourceMaster.category || 'General',
                                unitSize: targetMaster.unitSize || sourceMaster.unitSize || '',
                                unitType: targetMaster.unitType || sourceMaster.unitType || '',
                                imageUrl: targetMaster.imageUrl || sourceMaster.imageUrl || ''
                              });

                              const affected = allProducts.filter((p: any) => p.masterProductId === sourceMaster.id);
                              for (const offer of affected) {
                                await update(offer.id, { masterProductId: targetMaster.id });
                              }

                              await removeMaster(sourceMaster.id);
                              showToast(`Merged "${sourceMaster.canonicalName}" into "${targetMaster.canonicalName}" successfully!`, 'success');
                            } catch {
                              showToast('Merge operation failed.', 'error');
                            }
                          }
                        }}
                        className="p-2 py-1 bg-red-50 hover:bg-red-100 text-red-700 text-[10px] uppercase font-bold rounded-lg transition-colors shrink-0 whitespace-nowrap self-start sm:self-center"
                      >
                        Merge M2 ➔ M1
                      </button>
                    </div>
                  ))}
                  
                  {masterDuplicates.length === 0 && (
                    <div className="text-center py-6 text-slate-400 font-medium text-xs">
                      No possible duplicate master catalog products detected.
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}

          <div className="pt-4 flex justify-end">
            <button onClick={() => setIsMergeModalOpen(false)} className="px-6 py-2 bg-slate-900 text-white rounded-xl font-bold text-sm hover:bg-slate-800">Close</button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
