import React, { useState, useMemo, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Filter, MoreVertical, FileText, CheckCircle, Clock, ScanLine, Trash2, Search, Upload } from 'lucide-react';
import { useCollection } from '../lib/firebase-hooks';
import { Modal } from '../components/Modal';
import { useAuth } from '../contexts/AuthContext';
import { useToast } from '../contexts/ToastContext';
import { ref, uploadBytesResumable, getDownloadURL } from 'firebase/storage';
import { collection, query, where, getDocs, writeBatch } from 'firebase/firestore';
import { storage, db } from '../firebase';
import { trackEvent } from '../lib/analytics';

const StatusBadge = ({ status }: { status: string }) => {
  switch (status) {
    case 'published':
      return <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-[10px] font-bold uppercase tracking-wide bg-green-100 text-green-700"><CheckCircle className="w-3 h-3"/> Published</span>;
    case 'pending':
    case 'pending_review':
    case 'pending_admin_review':
    case 'submitted':
    case 'draft':
      return <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-[10px] font-bold uppercase tracking-wide bg-orange-100 text-orange-700"><Clock className="w-3 h-3"/> {status.replace(/_/g, ' ')}</span>;
    case 'ocr_review':
    case 'processing':
      return <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-[10px] font-bold uppercase tracking-wide bg-indigo-100 text-indigo-700"><ScanLine className="w-3 h-3"/> {status.replace(/_/g, ' ')}</span>;
    case 'rejected':
      return <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-[10px] font-bold uppercase tracking-wide bg-red-100 text-red-700">Rejected</span>;
    case 'expired':
      return <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-[10px] font-bold uppercase tracking-wide bg-slate-200 text-slate-500">Expired</span>;
    default:
      return <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-[10px] font-bold uppercase tracking-wide bg-slate-100 text-slate-700">{status}</span>;
  }
};

export default function Flyers() {
  const navigate = useNavigate();
  const { user, isAdminUser, isRetailerUser } = useAuth();
  const { showToast } = useToast();
  const filterKey = `${user.role || ''}:${user.retailerId || ''}`;
  const { data: allFlyers, loading, add, remove, update } = useCollection<any>('flyers', {
    constraints: isRetailerUser && user.retailerId ? [where('retailerId', '==', user.retailerId)] : [],
    enabled: Boolean(user.role),
    filterKey
  });
  const { data: retailers } = useCollection<any>('retailers', {
    constraints: isRetailerUser ? [where('status', '==', 'published')] : [],
    enabled: Boolean(user.role),
    filterKey
  });
  const [searchQuery, setSearchQuery] = useState('');
  const [isUploading, setIsUploading] = useState(false);
  const [newFlyer, setNewFlyer] = useState({ title: '', status: 'processing', validFrom: '', validTo: '' });
  const [selectedRetailer, setSelectedRetailer] = useState(isRetailerUser ? user.retailerId : '');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadError, setUploadError] = useState('');
  const [isUploadingFile, setIsUploadingFile] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [reviewFlyerId, setReviewFlyerId] = useState<string | null>(null);
  const [reviewStatus, setReviewStatus] = useState<'approved' | 'rejected'>('approved');
  const [reviewComments, setReviewComments] = useState('');

  const parseTimestamp = (val: unknown): number | null => {
    if (typeof val === 'number' && Number.isFinite(val) && val > 0) return val;
    if (typeof val === 'string' && val.trim()) {
      const num = Number(val);
      if (Number.isFinite(num) && num > 0) return num;
      const parsed = Date.parse(val);
      if (Number.isFinite(parsed)) return parsed;
    }
    return null;
  };

  const formatDateValue = (val: unknown): string => {
    const ts = parseTimestamp(val);
    if (ts === null) return '-';
    return new Date(ts).toLocaleDateString('en-GB', {
      day: 'numeric',
      month: 'short',
      year: 'numeric'
    });
  };

  const flyers = useMemo(() => {
    const nowMs = Date.now();
    const processed = allFlyers.map((f: any) => {
      const validToTs = parseTimestamp(f.validTo);
      if (validToTs !== null && validToTs < nowMs && f.status === 'published') {
        return { ...f, status: 'expired' };
      }
      return f;
    });

    if (isAdminUser) return processed;
    return processed.filter((f: any) => f.retailerId === user.retailerId);
  }, [allFlyers, user, isAdminUser]);

  const filteredFlyers = useMemo(() => {
    return flyers.filter((f: any) => {
      const matchSearch = f.title?.toLowerCase().includes(searchQuery.toLowerCase()) || 
                          f.retailerId?.toLowerCase().includes(searchQuery.toLowerCase());
      return matchSearch;
    });
  }, [flyers, searchQuery]);

  const handleDelete = async (id: string, title: string) => {
    if (window.confirm(`Are you sure you want to delete flyer "${title}"? This cannot be undone.`)) {
      try {
        await remove(id);
        showToast('Flyer deleted successfully', 'success');
      } catch (e) {
        showToast('Failed to delete flyer: Insufficient permissions or network err', 'error');
      }
    }
  };

  const handleFileSelection = (file: File) => {
    const validTypes = ['image/jpeg', 'image/png', 'application/pdf'];
    if (!validTypes.includes(file.type)) {
      setUploadError('Invalid file type. Please upload a PDF, JPG, or PNG.');
      setSelectedFile(null);
      return;
    }
    if (file.size > 20 * 1024 * 1024) { // 20MB
      setUploadError('File size exceeds 20MB limit.');
      setSelectedFile(null);
      return;
    }
    setUploadError('');
    setSelectedFile(file);
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      handleFileSelection(e.target.files[0]);
    }
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileSelection(e.dataTransfer.files[0]);
    }
  };

  const handleUploadFlyer = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!selectedFile) {
      setUploadError('Please select a file to upload.');
      return;
    }

    setIsUploadingFile(true);
    setUploadError('');

    try {
      const extension = selectedFile.name.split('.').pop();
      const actualRetailerId = isRetailerUser ? user.retailerId : selectedRetailer;
      const fileName = `flyers/${actualRetailerId || 'unknown'}/${Date.now()}_${Math.random().toString(36).substring(2)}.${extension}`;
      const storageRef = ref(storage, fileName);
      
      const uploadTask = uploadBytesResumable(storageRef, selectedFile);

      uploadTask.on(
        'state_changed',
        (snapshot) => {
          const progress = (snapshot.bytesTransferred / snapshot.totalBytes) * 100;
          setUploadProgress(progress);
        },
        (error) => {
          console.error("Storage upload error:", error);
          setUploadError('Failed to upload file. Please try again.');
          setIsUploadingFile(false);
        },
        async () => {
          try {
            const downloadURL = await getDownloadURL(uploadTask.snapshot.ref);
            const isPdf = selectedFile.type === 'application/pdf';
            const now = Date.now();
            const validFromMs = newFlyer.validFrom ? (Date.parse(newFlyer.validFrom) || now) : now;
            const validToMs = newFlyer.validTo ? (Date.parse(newFlyer.validTo) || (now + 14 * 86400000)) : (now + 14 * 86400000);
            
            await add({ 
              ...newFlyer, 
              retailerId: isRetailerUser ? user.retailerId : selectedRetailer,
              status: 'processing',
              validFrom: validFromMs,
              validTo: validToMs,
              [isPdf ? 'pdfUrl' : 'imageUrl']: downloadURL,
              pages: 1,
              views: 0,
              clicks: 0,
              createdBy: user.email,
              createdAt: now,
              updatedAt: now,
              auditLog: [{
                status: 'processing',
                user: user.email,
                timestamp: new Date(now).toISOString(),
                action: 'Uploaded'
              }]
            });
            
            showToast('Flyer uploaded successfully. Our AI will process it shortly.', 'success');
          } catch (error) {
            console.error(error);
            showToast('Failed to save flyer details.', 'error');
          }
          
          setIsUploading(false);
          setIsUploadingFile(false);
          setNewFlyer({ title: '', status: 'processing', validFrom: '', validTo: '' });
          setSelectedRetailer(isRetailerUser ? user.retailerId : '');
          setSelectedFile(null);
          setUploadProgress(0);
          if (fileInputRef.current) fileInputRef.current.value = '';
        }
      );
    } catch (error) {
      console.error("Error creating flyer doc:", error);
      setUploadError('Failed to save flyer details.');
      showToast('Failed to start upload process.', 'error');
      setIsUploadingFile(false);
    }
  };

  const handleReviewSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!reviewFlyerId) return;

    const targetFlyer = allFlyers.find((f: any) => f.id === reviewFlyerId);
    if (!targetFlyer) return;

    try {
      const actionLabel = reviewStatus === 'approved' ? 'Approved flyer' : 'Rejected flyer';
      const newStatus = reviewStatus === 'approved' ? 'published' : 'rejected';

      const newAuditLog = {
        status: newStatus,
        user: user.email,
        timestamp: new Date().toISOString(),
        action: actionLabel,
        comments: reviewComments
      };

      const auditLogHistory = targetFlyer.auditLog || [];

      await update(reviewFlyerId, {
        status: newStatus,
        updatedAt: new Date().toISOString(),
        auditLog: [...auditLogHistory, newAuditLog]
      });
      
      const q = query(collection(db, 'products'), where('flyerId', '==', reviewFlyerId));
      const querySnapshot = await getDocs(q);
      const batch = writeBatch(db);
      
      querySnapshot.forEach((docSnap) => {
        batch.update(docSnap.ref, { status: productStatusForFlyerState(newStatus) });
      });
      
      await batch.commit();
      
      showToast('Flyer review submitted successfully.', 'success');
    } catch (err) {
      console.error('Failed to process review', err);
      showToast('Failed to process review. Please try again.', 'error');
    }

    setReviewFlyerId(null);
    setReviewComments('');
  };
  
  const productStatusForFlyerState = (flyerStatus: string) => {
    if (flyerStatus === 'published') return 'published';
    if (flyerStatus === 'rejected') return 'rejected';
    return 'pending_admin_review';
  };

  const getRetailerName = (id: string) => {
    const r = retailers.find((ret: any) => ret.id === id);
    return r ? r.name : id;
  };

  const pendingJobs = flyers.filter((f: any) => f.status === 'processing' || f.status === 'ocr_review' || f.status === 'draft' || f.status === 'pending_review' || f.status === 'pending');
  const targetReviewFlyer = flyers.find((f: any) => f.id === reviewFlyerId);

  return (
    <div className="flex-1 flex flex-col h-full">
      <div className="mb-6 flex sm:flex-row flex-col justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">{isAdminUser ? 'Flyer Management' : 'My Flyers'}</h1>
          <p className="text-slate-500 mt-1 text-sm">Upload and manage promotional flyers.</p>
        </div>
        <div className="flex items-center gap-3 w-full sm:w-auto">
          <button className="flex items-center gap-2 px-4 py-2 border border-slate-200 text-slate-700 bg-white rounded-xl hover:bg-slate-50 transition-colors font-semibold text-sm">
            <Filter className="w-4 h-4" />
            Filter
          </button>
          <button onClick={() => setIsUploading(true)} className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors font-semibold text-sm w-full sm:w-auto justify-center">
            <Plus className="w-4 h-4" />
            Upload Flyer
          </button>
        </div>
      </div>

      <div className="bg-white border border-slate-200 rounded-3xl shadow-sm overflow-hidden flex-1 flex flex-col">
        <div className="p-4 border-b border-slate-100 flex flex-col sm:flex-row gap-4 items-center justify-between shrink-0">
            <div className="relative flex-1 w-full max-w-md">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 w-4 h-4" />
              <input 
                type="text" 
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search flyers by title or retailer..." 
                className="w-full pl-9 pr-4 py-2 bg-slate-50 border-none rounded-full focus:ring-2 focus:ring-indigo-500 text-sm outline-none transition-all"
              />
            </div>
        </div>

        <div className="overflow-x-auto flex-1">
          {loading ? (
             <div className="p-12 text-center text-slate-500 font-medium animate-pulse">Loading flyers...</div>
          ) : filteredFlyers.length === 0 ? (
             <div className="p-12 text-center flex flex-col items-center justify-center h-full">
               <FileText className="w-12 h-12 text-slate-300 mb-4" />
               <h3 className="text-lg font-bold text-slate-900 mb-1">No flyers found</h3>
               <p className="text-sm text-slate-500">Upload your first flyer to begin OCR extraction.</p>
             </div>
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50/50 border-b border-slate-200">
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">Flyer Title</th>
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">Retailer</th>
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">Status</th>
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">Uploaded</th>
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">Validity</th>
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">Views</th>
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">Clicks</th>
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredFlyers.map((flyer: any) => (
                  <tr key={flyer.id} className="hover:bg-slate-50 cursor-pointer transition-colors" onClick={async () => {
                    await update(flyer.id, { views: (flyer.views || 0) + 1 });
                    await trackEvent('flyer_view', { flyerId: flyer.id, title: flyer.title, retailerId: flyer.retailerId || '' });
                    showToast(`Flyer view logged: ${flyer.title}`, 'info');
                  }}>
                    <td className="py-4 px-6">
                      <div className="flex items-center gap-3">
                        {flyer.imageUrl ? (
                          <img src={flyer.imageUrl} alt={flyer.title} className="w-10 h-10 rounded-xl object-cover border border-slate-200 shrink-0" />
                        ) : flyer.pdfUrl ? (
                          <div className="w-10 h-10 bg-red-50 rounded-xl flex items-center justify-center text-red-600 shrink-0 border border-red-100">
                            <FileText className="w-5 h-5" />
                          </div>
                        ) : (
                          <div className="w-10 h-10 bg-indigo-50 rounded-xl flex items-center justify-center text-indigo-600 shrink-0">
                            <FileText className="w-5 h-5" />
                          </div>
                        )}
                        <span className="font-semibold text-slate-900">{flyer.title}</span>
                      </div>
                    </td>
                    <td className="py-4 px-6 text-sm text-slate-600 font-medium">{getRetailerName(flyer.retailerId) || 'Unknown'}</td>
                    <td className="py-4 px-6"><StatusBadge status={flyer.status} /></td>
                    <td className="py-4 px-6 text-sm text-slate-600">{formatDateValue(flyer.createdAt || flyer.uploadDate)}</td>
                    <td className="py-4 px-6 text-sm text-slate-600">{flyer.validFrom && flyer.validTo ? `${formatDateValue(flyer.validFrom)} – ${formatDateValue(flyer.validTo)}` : (flyer.date || formatDateValue(flyer.validFrom || flyer.validTo))}</td>
                    <td className="py-4 px-6 text-sm text-slate-600 font-bold">{flyer.views || 0}</td>
                    <td className="py-4 px-6 text-sm text-slate-600 font-bold">{flyer.clicks || 0}</td>
                    <td className="py-4 px-6 text-right">
                      <div className="flex items-center justify-end gap-2">
                        {isAdminUser && (flyer.status === 'pending' || flyer.status === 'pending_review' || flyer.status === 'pending_admin_review') && (
                          <button onClick={() => setReviewFlyerId(flyer.id)} className="text-slate-400 hover:text-indigo-600 p-1.5 rounded-lg hover:bg-indigo-50 transition-colors" title="Review Flyer">
                            <CheckCircle className="w-4 h-4" />
                          </button>
                        )}
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            navigate(`/ocr/${flyer.id}`);
                          }}
                          className="text-slate-400 hover:text-slate-900 p-1.5 rounded-lg hover:bg-slate-100 transition-colors"
                          title="Manage"
                        >
                          <MoreVertical className="w-5 h-5" />
                        </button>
                        <button onClick={() => handleDelete(flyer.id, flyer.title)} className="text-slate-400 hover:text-red-600 p-1.5 rounded-lg hover:bg-red-50 transition-colors" title="Delete">
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
        <div className="px-6 py-4 border-t border-slate-200 bg-slate-50/50 text-xs text-slate-500 flex justify-between items-center font-medium">
          <span>Showing {filteredFlyers.length} flyers</span>
          <div className="flex items-center gap-2">
            <button disabled className="px-3 py-1.5 border border-slate-200 bg-white rounded-lg text-slate-600 hover:bg-slate-50 disabled:opacity-50 transition-colors">Previous</button>
            <button disabled className="px-3 py-1.5 border border-slate-200 bg-white rounded-lg text-slate-600 hover:bg-slate-50 disabled:opacity-50 transition-colors">Next</button>
          </div>
        </div>
      </div>

      <Modal isOpen={isUploading} onClose={() => setIsUploading(false)} title="Upload Flyer">
        <form onSubmit={handleUploadFlyer} className="space-y-4">
          <div>
            <label className="block text-sm font-semibold text-slate-900 mb-1">Flyer Title</label>
            <input 
              type="text" 
              required
              value={newFlyer.title}
              onChange={(e) => setNewFlyer({...newFlyer, title: e.target.value})}
              className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors"
              placeholder="e.g. Weekend Mega Sale"
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-semibold text-slate-900 mb-1">Valid From</label>
              <input 
                type="date" 
                required
                value={newFlyer.validFrom}
                onChange={(e) => setNewFlyer({...newFlyer, validFrom: e.target.value})}
                className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors"
              />
            </div>
            <div>
              <label className="block text-sm font-semibold text-slate-900 mb-1">Valid To</label>
              <input 
                type="date" 
                required
                value={newFlyer.validTo}
                onChange={(e) => setNewFlyer({...newFlyer, validTo: e.target.value})}
                className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors"
              />
            </div>
          </div>
          {isAdminUser && (
            <div className="mb-4">
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
             <label className="block text-sm font-semibold text-slate-900 mb-1">Flyer Document (PDF/Images)</label>
             <div 
               className="border-2 border-dashed border-indigo-200 bg-indigo-50/30 rounded-xl p-8 flex flex-col items-center justify-center text-center cursor-pointer hover:bg-indigo-50/50 transition"
               onClick={() => fileInputRef.current?.click()}
               onDragOver={(e) => e.preventDefault()}
               onDrop={handleDrop}
             >
                {selectedFile ? (
                  <div className="flex flex-col items-center">
                    <div className="text-sm font-bold text-slate-900">{selectedFile.name}</div>
                    <div className="text-xs text-slate-500 font-medium mt-1">{(selectedFile.size / (1024 * 1024)).toFixed(2)} MB</div>
                    {selectedFile.type.startsWith('image/') && (
                      <img src={URL.createObjectURL(selectedFile)} alt="Preview" className="max-h-32 mt-4 rounded-lg object-contain shadow-sm border border-slate-200" />
                    )}
                  </div>
                ) : (
                  <>
                    <Upload className="w-8 h-8 text-indigo-400 mb-3 opacity-70" />
                    <p className="text-sm font-bold text-slate-900 mb-1">Click to select or drag and drop file here</p>
                    <p className="text-xs text-slate-500 max-w-[200px] mb-2">Supports PDF, JPG, PNG (Max 20MB)</p>
                    <p className="text-xs text-indigo-600 bg-indigo-100 px-3 py-1 rounded-full font-medium">Use Camera on Mobile</p>
                  </>
                )}
             </div>
             <input 
               type="file" 
               ref={fileInputRef} 
               className="hidden" 
               accept="image/jpeg, image/png, application/pdf" 
               capture="environment"
               onChange={handleFileChange} 
             />
             {uploadError && <p className="text-red-500 text-xs mt-2 font-medium bg-red-50 p-2 rounded-lg">{uploadError}</p>}
             {isUploadingFile && (
               <div className="mt-4">
                 <div className="flex justify-between text-xs font-semibold mb-1 text-slate-700">
                   <span>Uploading...</span>
                   <span>{Math.round(uploadProgress)}%</span>
                 </div>
                 <div className="w-full bg-slate-200 rounded-full h-2 overflow-hidden">
                   <div className="bg-indigo-600 h-2 rounded-full transition-all duration-300 shadow-sm" style={{ width: `${uploadProgress}%` }}></div>
                 </div>
               </div>
             )}
          </div>
          <div className="pt-4 flex items-center justify-end gap-3">
            <button 
              type="button" 
              onClick={() => { setIsUploading(false); setUploadError(''); setSelectedFile(null); }}
              className="px-4 py-2 text-sm font-bold text-slate-600 hover:bg-slate-100 rounded-lg transition-colors disabled:opacity-50"
              disabled={isUploadingFile}
            >
              Cancel
            </button>
            <button 
              type="submit" 
              className="px-6 py-2 text-sm font-bold bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors disabled:opacity-50 flex items-center gap-2"
              disabled={isUploadingFile || !selectedFile || (!newFlyer.title || !newFlyer.validFrom || !newFlyer.validTo)}
            >
              {isUploadingFile ? (
                <>
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                  Uploading
                </>
              ) : 'Upload & Process'}
            </button>
          </div>
        </form>
      </Modal>

      <Modal isOpen={reviewFlyerId !== null} onClose={() => setReviewFlyerId(null)} title="Admin Review">
        {targetReviewFlyer && (
          <div className="mb-6 p-4 bg-slate-50 border border-slate-200 rounded-xl">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-3">Audit Log</h4>
            {targetReviewFlyer.auditLog && targetReviewFlyer.auditLog.length > 0 ? (
              <div className="space-y-3">
                {targetReviewFlyer.auditLog.map((log: any, idx: number) => (
                  <div key={idx} className="flex flex-col text-sm">
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-slate-900">{log.action}</span>
                      <span className="text-xs text-slate-500">•</span>
                      <span className="text-xs text-slate-500">{new Date(log.timestamp).toLocaleString()}</span>
                    </div>
                    <div className="text-slate-600 text-xs mt-0.5"><span className="font-medium">User:</span> {log.user}</div>
                    {log.comments && <div className="text-slate-600 text-xs mt-0.5 italic">"{log.comments}"</div>}
                  </div>
                ))}
              </div>
            ) : (
              <span className="text-xs text-slate-500 italic">No audit log history</span>
            )}
          </div>
        )}
        <form onSubmit={handleReviewSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-semibold text-slate-900 mb-1">Status</label>
            <select 
              value={reviewStatus}
              onChange={(e) => setReviewStatus(e.target.value as any)}
              className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors"
            >
              <option value="approved">Approve & Publish</option>
              <option value="rejected">Reject</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-semibold text-slate-900 mb-1">Comments (optional)</label>
            <textarea 
              value={reviewComments}
              onChange={(e) => setReviewComments(e.target.value)}
              className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors"
              placeholder="Provide reason for rejection or approval notes..."
              rows={3}
            />
          </div>
          <div className="pt-4 flex items-center justify-end gap-3">
            <button 
              type="button" 
              onClick={() => setReviewFlyerId(null)}
              className="px-4 py-2 text-sm font-bold text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
            >
              Cancel
            </button>
            <button 
              type="submit" 
              className={`px-6 py-2 text-sm font-bold text-white rounded-xl transition-colors ${
                reviewStatus === 'approved' ? 'bg-green-600 hover:bg-green-700' : 'bg-red-600 hover:bg-red-700'
              }`}
            >
              Confirm
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
