import { useState, useEffect, useMemo, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  ArrowLeft, CheckCircle, Save, AlertTriangle, Image as ImageIcon, 
  Search, Plus, Trash2, ShieldCheck, X, Activity, Sparkles, 
  ChevronLeft, ChevronRight, RefreshCw, ZoomIn, ZoomOut, Check
} from 'lucide-react';
import { useCollection } from '../lib/firebase-hooks';
import { useAuth } from '../contexts/AuthContext';
import { useToast } from '../contexts/ToastContext';
import { findBestMasterMatch } from '../lib/matching';
import { trackEvent } from '../lib/analytics';
import { where } from 'firebase/firestore';
import { 
  renderPdfToCanvases, 
  extractProductsFromCanvas, 
  ExtractedProductItem 
} from '../lib/flyerExtractor';

export default function OcrReview() {
  const { jobId } = useParams();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const { user, isAdminUser, isRetailerUser } = useAuth();
  const { data: flyers, update: updateFlyer } = useCollection<any>('flyers', {
    constraints: isRetailerUser && user.retailerId ? [where('retailerId', '==', user.retailerId)] : [],
    enabled: Boolean(user.role),
    filterKey: `${user.role}:${user.retailerId || ''}`
  });
  const { add: addProduct } = useCollection('products', { enabled: false });
  const { data: masterProducts } = useCollection<any>('masterProducts');

  const [searchQuery, setSearchQuery] = useState('');
  const [products, setProducts] = useState<ExtractedProductItem[]>([]);
  
  const [isOcrProcessing, setIsOcrProcessing] = useState(false);
  const [ocrProgress, setOcrProgress] = useState('');
  const [engineUsed, setEngineUsed] = useState<'gemini' | 'tesseract'>('gemini');

  // Multi-page state
  const [pagesCanvases, setPagesCanvases] = useState<HTMLCanvasElement[]>([]);
  const [currentPageIndex, setCurrentPageIndex] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [displayedImage, setDisplayedImage] = useState<string | null>(null);

  // Hover and selection interaction between document and product list
  const [hoveredProductId, setHoveredProductId] = useState<string | null>(null);
  const [selectedProductId, setSelectedProductId] = useState<string | null>(null);

  // Zoom & Pan state
  const [zoomLevel, setZoomLevel] = useState(1.0);

  const flyer = useMemo(() => flyers.find(f => f.id === jobId), [flyers, jobId]);
  const imageContainerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (flyer && pagesCanvases.length === 0 && !isOcrProcessing && !displayedImage) {
      if (flyer.pdfUrl) {
        loadPdf(flyer.pdfUrl);
      } else if (flyer.imageUrl) {
        loadImage(flyer.imageUrl);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [flyer]);

  const loadPdf = async (pdfUrl: string) => {
    setIsOcrProcessing(true);
    setOcrProgress('Rendering PDF pages...');
    try {
      const { canvases, totalPages: count } = await renderPdfToCanvases(pdfUrl, 5);
      setPagesCanvases(canvases);
      setTotalPages(count);
      
      if (canvases.length > 0) {
        const firstCanvas = canvases[0];
        setDisplayedImage(firstCanvas.toDataURL('image/jpeg', 0.9));
        await runExtractionOnCanvas(firstCanvas, 1);
      }
    } catch (e: any) {
      console.error('Failed to render PDF:', e);
      setOcrProgress('Could not load PDF document.');
      showToast('Failed to parse PDF document.', 'error');
      setIsOcrProcessing(false);
    }
  };

  const loadImage = async (imageUrl: string) => {
    setIsOcrProcessing(true);
    setOcrProgress('Loading flyer image...');
    try {
      const img = new Image();
      img.crossOrigin = 'anonymous';
      img.src = imageUrl;
      await new Promise((resolve, reject) => {
        img.onload = resolve;
        img.onerror = reject;
      });

      const canvas = document.createElement('canvas');
      canvas.width = img.naturalWidth || img.width;
      canvas.height = img.naturalHeight || img.height;
      const ctx = canvas.getContext('2d');
      ctx?.drawImage(img, 0, 0);

      setPagesCanvases([canvas]);
      setTotalPages(1);
      setDisplayedImage(imageUrl);
      await runExtractionOnCanvas(canvas, 1);
    } catch (e: any) {
      console.error('Failed to load image:', e);
      setOcrProgress('Could not load image.');
      showToast('Failed to load flyer image.', 'error');
      setIsOcrProcessing(false);
    }
  };

  const runExtractionOnCanvas = async (canvas: HTMLCanvasElement, pageNum: number) => {
    setIsOcrProcessing(true);
    try {
      const { products: extracted, engineUsed: engine } = await extractProductsFromCanvas(
        canvas,
        flyer?.retailerId || '',
        flyer?.id || '',
        pageNum,
        msg => setOcrProgress(msg)
      );

      setEngineUsed(engine);

      // Enhance with master catalog matches
      const enhanced = extracted.map(item => {
        const matchInfo = matchProductToMaster(item.name, undefined, item.brand, item.unitSize, item.unitType);
        return {
          ...item,
          matchedId: matchInfo.masterId,
          confidence: matchInfo.masterId ? Math.max(item.confidence, matchInfo.confidence) : item.confidence
        };
      });

      // Append or replace depending on page
      setProducts(prev => {
        const otherPages = prev.filter(p => p.sourcePage !== pageNum);
        return [...otherPages, ...enhanced];
      });

      showToast(`Extracted ${extracted.length} products with ${engine === 'gemini' ? 'Gemini AI Vision' : 'Smart Grid OCR'}!`, 'success');
    } catch (err: any) {
      console.error('Extraction error:', err);
      showToast('Error during automatic product extraction.', 'error');
    } finally {
      setIsOcrProcessing(false);
    }
  };

  const handlePageChange = async (newIndex: number) => {
    if (newIndex < 0 || newIndex >= pagesCanvases.length) return;
    setCurrentPageIndex(newIndex);
    const canvas = pagesCanvases[newIndex];
    setDisplayedImage(canvas.toDataURL('image/jpeg', 0.9));

    // If this page hasn't been extracted yet, extract it automatically
    const existing = products.filter(p => p.sourcePage === newIndex + 1);
    if (existing.length === 0) {
      await runExtractionOnCanvas(canvas, newIndex + 1);
    }
  };

  const reRunExtraction = () => {
    if (pagesCanvases[currentPageIndex]) {
      runExtractionOnCanvas(pagesCanvases[currentPageIndex], currentPageIndex + 1);
    }
  };

  const matchProductToMaster = (name: string, barcode?: string, brand?: string, unitSize?: string, unitType?: string) => {
    if (!masterProducts || masterProducts.length === 0) return { masterId: null, confidence: 50 };
    const res = findBestMasterMatch({ name, barcode, brand, unitSize, unitType }, masterProducts);
    return {
      masterId: res.masterId,
      confidence: res.masterId ? res.confidence : 75
    };
  };

  const toggleEdit = (id: string) => {
    setProducts(products.map(p => (p.id === id ? { ...p, isEdit: !p.isEdit } : p)));
  };

  const updateProduct = (id: string, field: keyof ExtractedProductItem, value: any) => {
    setProducts(products.map(p => (p.id === id ? { ...p, [field]: value } : p)));
  };

  const removeProduct = (id: string) => {
    setProducts(products.filter(p => p.id !== id));
  };

  const addMissingItem = () => {
    const newItem: ExtractedProductItem = {
      id: `manual-${Date.now()}`,
      name: 'New Product Deal',
      brand: '',
      category: 'General',
      currentPrice: 0.0,
      oldPrice: 0.0,
      unitSize: '',
      unitType: '',
      discount: '',
      imageUrl: displayedImage || '',
      confidence: 100,
      sourcePage: currentPageIndex + 1,
      isEdit: true
    };
    setProducts([newItem, ...products]);
  };

  const filteredProducts = useMemo(() => {
    return products.filter(p => 
      p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.brand.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.category.toLowerCase().includes(searchQuery.toLowerCase())
    );
  }, [products, searchQuery]);

  const saveDraft = async () => {
    if (jobId) {
      try {
        const newAuditLog = {
          status: 'draft',
          user: user?.email || 'Unknown',
          timestamp: new Date().toISOString(),
          action: 'Saved Draft'
        };
        const auditLogHistory = flyer?.auditLog || [];
        await updateFlyer(jobId, { 
          status: 'draft',
          updatedAt: new Date().toISOString(),
          auditLog: [...auditLogHistory, newAuditLog]
        });
        showToast('Draft saved successfully', 'success');
      } catch (err) {
        showToast('Failed to save draft', 'error');
        return;
      }
    }
    navigate('/ocr');
  };

  const handleBulkPublish = async () => {
    const productStatus = isAdminUser ? 'published' : 'pending_admin_review';

    try {
      showToast('Publishing products with cropped catalogue images...', 'info');
      
      // Bulk insert all products with their real cropped images
      await Promise.all(
        products.map(p =>
          addProduct({
            name: p.name,
            currentPrice: Number(p.currentPrice) || 0,
            oldPrice: Number(p.oldPrice) || null,
            category: p.category || 'General',
            brand: p.brand || 'Flyer Deal',
            retailerId: flyer?.retailerId || 'Unknown Retailer',
            flyerId: flyer?.id || '',
            unitSize: p.unitSize ? parseFloat(p.unitSize) || 1.0 : 1.0,
            unitType: p.unitType || 'unit',
            validFrom: flyer?.validFrom || Date.now(),
            validTo: flyer?.validTo || (Date.now() + 14 * 86400000),
            status: productStatus,
            masterProductId: (p as any).matchedId || '',
            barcode: '',
            imageUrl: p.imageUrl || '' // Real cropped catalogue image
          })
        )
      );

      if (jobId) {
        const nextFlyerStatus = isAdminUser ? 'published' : 'pending_admin_review';
        const actionLabel = isAdminUser ? 'Published via Automated Extraction' : 'Submitted for Review';

        const newAuditLog = {
          status: nextFlyerStatus,
          user: user?.email || 'Admin',
          timestamp: new Date().toISOString(),
          action: actionLabel
        };

        const auditLogHistory = flyer?.auditLog || [];
        await updateFlyer(jobId, {
          status: nextFlyerStatus,
          updatedAt: new Date().toISOString(),
          auditLog: [...auditLogHistory, newAuditLog]
        });

        await trackEvent('ocr_metrics', {
          flyerId: jobId,
          totalProducts: products.length,
          engineUsed,
          timestamp: Date.now()
        });
      }

      showToast(isAdminUser ? `All ${products.length} products published with real images!` : 'Submitted for admin review', 'success');
      navigate(isAdminUser ? '/products' : '/flyers');
    } catch (e) {
      console.error(e);
      showToast('Failed to publish products. Please check connection.', 'error');
    }
  };

  if (!flyer) {
    return (
      <div className="flex-1 flex items-center justify-center p-12">
        <Activity className="w-8 h-8 text-indigo-600 animate-spin mr-3" />
        <span className="text-slate-600 font-semibold">Loading flyer data...</span>
      </div>
    );
  }

  // Active products on current page for bounding box overlay
  const currentPageProducts = products.filter(p => p.sourcePage === currentPageIndex + 1);

  return (
    <div className="flex-1 flex flex-col h-full bg-slate-50 -m-6 p-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-4">
        <div className="flex items-center gap-4">
          <button onClick={() => navigate('/ocr')} className="p-2.5 bg-white border border-slate-200 rounded-xl hover:bg-slate-50 transition-colors shadow-xs">
            <ArrowLeft className="w-5 h-5 text-slate-700" />
          </button>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-xl font-bold text-slate-900">{flyer.title || 'Flyer Extraction'}</h1>
              <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold uppercase tracking-wider ${
                engineUsed === 'gemini' ? 'bg-emerald-100 text-emerald-800' : 'bg-indigo-100 text-indigo-800'
              }`}>
                <Sparkles className="w-3 h-3" /> {engineUsed === 'gemini' ? 'Gemini Flash AI' : 'Smart Grid OCR'}
              </span>
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              {flyer.retailerId || 'Unknown Store'} • {products.length} products automatically captured with images
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2.5 w-full sm:w-auto justify-end">
          <button
            onClick={reRunExtraction}
            disabled={isOcrProcessing}
            className="flex items-center gap-1.5 px-3 py-2 bg-white border border-slate-200 text-slate-700 rounded-xl font-semibold text-xs hover:bg-slate-50 shadow-xs disabled:opacity-50"
            title="Re-extract this page"
          >
            <RefreshCw className={`w-3.5 h-3.5 text-slate-500 ${isOcrProcessing ? 'animate-spin' : ''}`} />
            Re-Extract
          </button>

          <button
            onClick={saveDraft}
            className="px-4 py-2 bg-white border border-slate-200 text-slate-700 rounded-xl font-bold text-xs uppercase tracking-wide hover:bg-slate-50 shadow-xs"
          >
            Save Draft
          </button>

          <button
            onClick={handleBulkPublish}
            disabled={isOcrProcessing || products.length === 0}
            className={`px-5 py-2 text-white rounded-xl font-bold text-xs uppercase tracking-wide flex items-center gap-2 shadow-sm transition-all hover:opacity-95 disabled:opacity-50 ${
              isAdminUser ? 'bg-emerald-600 hover:bg-emerald-700' : 'bg-indigo-600 hover:bg-indigo-700'
            }`}
          >
            <CheckCircle className="w-4 h-4" />
            {isAdminUser ? `Publish All (${products.length})` : `Submit for Review (${products.length})`}
          </button>
        </div>
      </div>

      {/* Main Workspace: Document on Left, Captured Products on Right */}
      <div className="flex-1 flex flex-col lg:flex-row gap-6 min-h-0">
        
        {/* Left Column: Document Viewer with Interactive Bounding Boxes */}
        <div className="flex-1 bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden flex flex-col min-h-0">
          <div className="px-5 py-3 border-b border-slate-100 bg-slate-50/70 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <ImageIcon className="w-4 h-4 text-indigo-600" />
              <span className="font-bold text-sm text-slate-800">Catalogue Page Source</span>
            </div>

            <div className="flex items-center gap-2">
              {totalPages > 1 && (
                <div className="flex items-center bg-white border border-slate-200 rounded-lg shadow-2xs mr-2">
                  <button
                    onClick={() => handlePageChange(currentPageIndex - 1)}
                    disabled={currentPageIndex === 0}
                    className="p-1 text-slate-500 hover:text-slate-800 disabled:opacity-30"
                  >
                    <ChevronLeft className="w-4 h-4" />
                  </button>
                  <span className="px-2 text-xs font-bold text-slate-700">
                    Page {currentPageIndex + 1} of {totalPages}
                  </span>
                  <button
                    onClick={() => handlePageChange(currentPageIndex + 1)}
                    disabled={currentPageIndex >= pagesCanvases.length - 1}
                    className="p-1 text-slate-500 hover:text-slate-800 disabled:opacity-30"
                  >
                    <ChevronRight className="w-4 h-4" />
                  </button>
                </div>
              )}

              <div className="flex items-center bg-white border border-slate-200 rounded-lg shadow-2xs">
                <button
                  onClick={() => setZoomLevel(prev => Math.max(0.6, prev - 0.2))}
                  className="p-1 text-slate-500 hover:text-slate-800"
                  title="Zoom Out"
                >
                  <ZoomOut className="w-4 h-4" />
                </button>
                <span className="px-2 text-[11px] font-bold text-slate-600">
                  {Math.round(zoomLevel * 100)}%
                </span>
                <button
                  onClick={() => setZoomLevel(prev => Math.min(2.0, prev + 0.2))}
                  className="p-1 text-slate-500 hover:text-slate-800"
                  title="Zoom In"
                >
                  <ZoomIn className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>

          <div
            ref={imageContainerRef}
            className="flex-1 bg-slate-100 relative overflow-auto flex items-center justify-center p-4 select-none"
          >
            {displayedImage ? (
              <div
                className="relative inline-block shadow-lg border border-slate-300 rounded-sm bg-white transition-transform duration-150 origin-top"
                style={{ transform: `scale(${zoomLevel})` }}
              >
                <img
                  src={displayedImage}
                  alt="Flyer Page"
                  className="max-w-none w-auto h-auto block max-h-[75vh]"
                  crossOrigin="anonymous"
                />

                {/* Interactive Bounding Boxes Overlay */}
                {currentPageProducts.map(p => {
                  if (!p.box2d) return null;
                  const [ymin, xmin, ymax, xmax] = p.box2d;
                  const isHovered = hoveredProductId === p.id;
                  const isSelected = selectedProductId === p.id;

                  return (
                    <div
                      key={p.id}
                      onClick={() => setSelectedProductId(p.id)}
                      onMouseEnter={() => setHoveredProductId(p.id)}
                      onMouseLeave={() => setHoveredProductId(null)}
                      style={{
                        top: `${ymin / 10}%`,
                        left: `${xmin / 10}%`,
                        width: `${(xmax - xmin) / 10}%`,
                        height: `${(ymax - ymin) / 10}%`
                      }}
                      className={`absolute cursor-pointer transition-all duration-150 rounded-sm flex flex-col justify-between p-1 ${
                        isSelected || isHovered
                          ? 'border-2 border-emerald-500 bg-emerald-500/20 shadow-md ring-2 ring-emerald-400/50 z-20'
                          : 'border border-indigo-400/80 bg-indigo-500/10 hover:border-emerald-400 hover:bg-emerald-500/15 z-10'
                      }`}
                    >
                      <span className="self-start text-[9px] font-bold bg-slate-900/90 text-white px-1 py-0.5 rounded shadow-xs truncate max-w-full">
                        AED {Number(p.currentPrice).toFixed(2)}
                      </span>
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="text-slate-400 font-bold uppercase tracking-wider text-sm flex items-center gap-2">
                <Activity className="w-5 h-5 animate-spin text-indigo-500" />
                Rendering Document Page...
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Captured Product Deals with Real Cropped Thumbnails */}
        <div className="w-full lg:w-[620px] flex flex-col flex-shrink-0 bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden min-h-0">
          <div className="px-5 py-4 border-b border-slate-100 bg-slate-50/70">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-5 h-5 text-emerald-600" />
                <h3 className="font-bold text-sm text-slate-800">
                  Captured Products ({filteredProducts.length})
                </h3>
              </div>
              
              {isOcrProcessing && (
                <div className="flex items-center gap-2 bg-indigo-50 border border-indigo-200 px-3 py-1 rounded-full">
                  <Activity className="w-3.5 h-3.5 text-indigo-600 animate-spin" />
                  <span className="text-[11px] font-bold text-indigo-700">{ocrProgress}</span>
                </div>
              )}
            </div>

            <div className="relative">
              <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 w-4 h-4" />
              <input
                type="text"
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                placeholder="Search products by title, brand, or category..."
                className="w-full pl-10 pr-4 py-2 bg-white border border-slate-200 rounded-xl text-xs font-medium focus:ring-2 focus:ring-indigo-500 outline-none shadow-2xs"
              />
            </div>
          </div>

          {/* Product Items List */}
          <div className="flex-1 overflow-y-auto p-4 space-y-3">
            {filteredProducts.length === 0 ? (
              <div className="p-8 text-center text-slate-500 text-xs">
                No matching products found. Click "Add Missing Item" to create one manually.
              </div>
            ) : (
              filteredProducts.map(p => {
                const isHovered = hoveredProductId === p.id;
                const isSelected = selectedProductId === p.id;

                return (
                  <div
                    key={p.id}
                    onMouseEnter={() => setHoveredProductId(p.id)}
                    onMouseLeave={() => setHoveredProductId(null)}
                    onClick={() => setSelectedProductId(p.id)}
                    className={`p-4 rounded-2xl border transition-all duration-150 relative group ${
                      isSelected
                        ? 'border-emerald-500 bg-emerald-50/40 shadow-sm ring-1 ring-emerald-400/40'
                        : isHovered
                        ? 'border-indigo-300 bg-indigo-50/30 shadow-xs'
                        : 'border-slate-200 bg-white hover:border-slate-300'
                    }`}
                  >
                    {p.isEdit ? (
                      /* Inline Edit Form */
                      <div className="space-y-3">
                        <div className="flex items-center justify-between mb-1">
                          <span className="text-[10px] font-bold text-indigo-600 uppercase tracking-widest">
                            Editing Product
                          </span>
                          <button onClick={() => toggleEdit(p.id)} className="text-slate-400 hover:text-slate-600">
                            <X className="w-4 h-4" />
                          </button>
                        </div>

                        <div className="flex gap-4 items-start">
                          {/* Image preview */}
                          {p.imageUrl && (
                            <img
                              src={p.imageUrl}
                              alt="Cropped Product"
                              className="w-20 h-20 object-contain rounded-xl border border-slate-200 bg-white p-1 shrink-0"
                            />
                          )}
                          <div className="flex-1 space-y-2">
                            <div>
                              <label className="text-[9px] uppercase font-bold text-slate-400">Title</label>
                              <input
                                type="text"
                                value={p.name}
                                onChange={e => updateProduct(p.id, 'name', e.target.value)}
                                className="w-full text-xs font-bold border-b border-slate-200 focus:border-indigo-500 outline-none py-0.5 bg-transparent"
                              />
                            </div>
                            <div className="grid grid-cols-2 gap-2">
                              <div>
                                <label className="text-[9px] uppercase font-bold text-slate-400">Brand</label>
                                <input
                                  type="text"
                                  value={p.brand}
                                  onChange={e => updateProduct(p.id, 'brand', e.target.value)}
                                  className="w-full text-xs border-b border-slate-200 focus:border-indigo-500 outline-none py-0.5 bg-transparent"
                                />
                              </div>
                              <div>
                                <label className="text-[9px] uppercase font-bold text-slate-400">Category</label>
                                <input
                                  type="text"
                                  value={p.category}
                                  onChange={e => updateProduct(p.id, 'category', e.target.value)}
                                  className="w-full text-xs border-b border-slate-200 focus:border-indigo-500 outline-none py-0.5 bg-transparent"
                                />
                              </div>
                            </div>
                          </div>
                        </div>

                        <div className="grid grid-cols-4 gap-2 pt-2 border-t border-slate-100">
                          <div>
                            <label className="text-[9px] uppercase font-bold text-slate-400">Price (AED)</label>
                            <input
                              type="number"
                              step="0.01"
                              value={p.currentPrice}
                              onChange={e => updateProduct(p.id, 'currentPrice', parseFloat(e.target.value) || 0)}
                              className="w-full text-xs font-bold text-emerald-600 border-b border-slate-200 focus:border-emerald-500 outline-none py-0.5 bg-transparent"
                            />
                          </div>
                          <div>
                            <label className="text-[9px] uppercase font-bold text-slate-400">Old Price</label>
                            <input
                              type="number"
                              step="0.01"
                              value={p.oldPrice || ''}
                              onChange={e => updateProduct(p.id, 'oldPrice', parseFloat(e.target.value) || 0)}
                              className="w-full text-xs text-slate-400 line-through border-b border-slate-200 focus:border-slate-500 outline-none py-0.5 bg-transparent"
                            />
                          </div>
                          <div>
                            <label className="text-[9px] uppercase font-bold text-slate-400">Size</label>
                            <input
                              type="text"
                              value={p.unitSize}
                              onChange={e => updateProduct(p.id, 'unitSize', e.target.value)}
                              className="w-full text-xs border-b border-slate-200 focus:border-indigo-500 outline-none py-0.5 bg-transparent"
                            />
                          </div>
                          <div>
                            <label className="text-[9px] uppercase font-bold text-slate-400">Unit Type</label>
                            <input
                              type="text"
                              value={p.unitType}
                              onChange={e => updateProduct(p.id, 'unitType', e.target.value)}
                              className="w-full text-xs border-b border-slate-200 focus:border-indigo-500 outline-none py-0.5 bg-transparent"
                            />
                          </div>
                        </div>

                        <div className="flex justify-end pt-1">
                          <button
                            onClick={() => toggleEdit(p.id)}
                            className="px-3 py-1 bg-indigo-600 text-white rounded-lg text-xs font-bold hover:bg-indigo-700 shadow-2xs"
                          >
                            Done
                          </button>
                        </div>
                      </div>
                    ) : (
                      /* Display Mode with Real Cropped Product Image */
                      <div className="flex items-start gap-3.5">
                        {/* Real Cropped Image Thumbnail */}
                        <div className="relative shrink-0">
                          {p.imageUrl ? (
                            <img
                              src={p.imageUrl}
                              alt={p.name}
                              className="w-20 h-20 object-contain rounded-xl border border-slate-200 bg-white p-1 shadow-2xs"
                            />
                          ) : (
                            <div className="w-20 h-20 bg-slate-100 rounded-xl flex items-center justify-center text-slate-300">
                              <ImageIcon className="w-6 h-6" />
                            </div>
                          )}
                          <span className="absolute bottom-1 right-1 text-[9px] font-bold bg-slate-900/80 text-white px-1 rounded">
                            P{p.sourcePage}
                          </span>
                        </div>

                        {/* Product Details */}
                        <div className="flex-1 min-w-0">
                          <div className="flex items-start justify-between gap-2 mb-1">
                            <div>
                              {p.brand && (
                                <span className="text-[10px] font-bold text-indigo-600 uppercase tracking-wide">
                                  {p.brand} • {p.category}
                                </span>
                              )}
                              <h4 className="font-bold text-xs text-slate-900 leading-tight line-clamp-2">
                                {p.name}
                              </h4>
                            </div>

                            {/* Actions */}
                            <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                              <button
                                onClick={e => {
                                  e.stopPropagation();
                                  toggleEdit(p.id);
                                }}
                                className="p-1 bg-white text-slate-500 shadow-xs border border-slate-200 rounded-lg hover:text-indigo-600"
                                title="Edit Details"
                              >
                                <Save className="w-3.5 h-3.5" />
                              </button>
                              <button
                                onClick={e => {
                                  e.stopPropagation();
                                  removeProduct(p.id);
                                }}
                                className="p-1 bg-white text-slate-500 shadow-xs border border-slate-200 rounded-lg hover:text-red-500"
                                title="Remove Item"
                              >
                                <Trash2 className="w-3.5 h-3.5" />
                              </button>
                            </div>
                          </div>

                          <div className="flex flex-wrap items-baseline gap-3 my-1.5">
                            <span className="text-sm font-bold text-emerald-600">
                              AED {Number(p.currentPrice).toFixed(2)}
                            </span>
                            {p.oldPrice && p.oldPrice > p.currentPrice && (
                              <span className="text-xs text-slate-400 line-through">
                                AED {Number(p.oldPrice).toFixed(2)}
                              </span>
                            )}
                            {p.unitSize && (
                              <span className="text-[11px] font-medium text-slate-500">
                                {p.unitSize} {p.unitType}
                              </span>
                            )}
                          </div>

                          <div className="flex items-center gap-2 mt-2">
                            {p.confidence >= 80 ? (
                              <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[9px] font-bold uppercase tracking-wider bg-emerald-100 text-emerald-800">
                                <CheckCircle className="w-2.5 h-2.5" /> Auto-Captured
                              </span>
                            ) : (
                              <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[9px] font-bold uppercase tracking-wider bg-amber-100 text-amber-800">
                                <AlertTriangle className="w-2.5 h-2.5" /> Review Price
                              </span>
                            )}

                            {(p as any).matchedId && (
                              <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[9px] font-bold uppercase tracking-wider bg-indigo-100 text-indigo-800">
                                <ShieldCheck className="w-2.5 h-2.5" /> Master Match
                              </span>
                            )}
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                );
              })
            )}

            <button
              onClick={addMissingItem}
              className="w-full py-3 border-2 border-dashed border-slate-200 bg-slate-50/50 rounded-2xl flex items-center justify-center gap-2 text-xs font-bold text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition-colors"
            >
              <Plus className="w-4 h-4" /> Add Missing Product
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
