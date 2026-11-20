import { useState, useRef } from 'react';
import { ScanLine, Upload, Camera, FileText, ShieldCheck, ArrowRight, Sparkles, Activity } from 'lucide-react';
import { useCollection } from '../lib/firebase-hooks';
import { useAuth } from '../contexts/AuthContext';
import { useToast } from '../contexts/ToastContext';
import { useNavigate } from 'react-router-dom';
import { renderPdfToCanvases } from '../lib/flyerExtractor';
import { functions, storage } from '../firebase';
import { ref, uploadBytes, getDownloadURL } from 'firebase/storage';
import { httpsCallable } from 'firebase/functions';
import { where } from 'firebase/firestore';

export default function OcrCenter() {
  const { user, isRetailerUser, isAdminUser, isModeratorTeam } = useAuth();
  const { data: flyers, loading, add: addFlyer } = useCollection<any>('flyers', {
    constraints: isRetailerUser && user.retailerId ? [where('retailerId', '==', user.retailerId)] : [],
    enabled: Boolean(user.role),
    filterKey: `${user.role}:${user.retailerId || ''}`
  });
  const { data: retailers } = useCollection<any>('retailers', {
    enabled: Boolean(user.role) && (isAdminUser || isModeratorTeam),
    filterKey: `ret:${user.role || ''}`
  });
  const { showToast } = useToast();
  const navigate = useNavigate();

  const fileInputRef = useRef<HTMLInputElement>(null);
  const [isProcessingUpload, setIsProcessingUpload] = useState(false);
  const [uploadStatus, setUploadStatus] = useState('');
  const [selectedRetailerId, setSelectedRetailerId] = useState(user.retailerId || '');

  const effectiveRetailerId = isRetailerUser ? (user.retailerId || '') : (selectedRetailerId || user.retailerId || '');

  const pendingJobs = flyers.filter(
    f => ['processing', 'ocr_review', 'draft', 'pending_review', 'pending', 'review_required', 'queued', 'extracting', 'pending_admin_review'].includes(f.status)
  );

  const handleFileSelected = async (file: File) => {
    if (!file) return;
    if (!effectiveRetailerId) {
      showToast('Select a retailer before uploading a flyer.', 'error');
      return;
    }
    const maxBytes = 25 * 1024 * 1024;
    if (file.size > maxBytes) {
      showToast('File exceeds the 25 MB upload limit.', 'error');
      return;
    }

    setIsProcessingUpload(true);
    setUploadStatus('Reading uploaded file...');

    try {
      const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');
      let coverImageUrl = '';
      let fileUrl = '';

      setUploadStatus('Uploading to secure tenant storage...');
      const safeName = file.name.replace(/[^a-zA-Z0-9._-]/g, '_');
      const storagePath = `private_extractions/${effectiveRetailerId}/${Date.now()}_${safeName}`;
      const storageRef = ref(storage, storagePath);
      const snapshot = await uploadBytes(storageRef, file);
      fileUrl = await getDownloadURL(snapshot.ref);

      // Generate cover thumbnail and upload to Storage (avoid huge base64 strings inside Firestore)
      let pageCount = 1;
      if (isPdf) {
        setUploadStatus('Rendering PDF cover preview...');
        const { canvases, totalPages } = await renderPdfToCanvases(file, 1, 1.2);
        pageCount = totalPages || 1;
        if (canvases.length > 0) {
          const coverBlob = await new Promise<Blob | null>((resolve) =>
            canvases[0].toBlob((b) => resolve(b), 'image/jpeg', 0.85)
          );
          if (coverBlob) {
            const coverRef = ref(storage, `flyers/${effectiveRetailerId}/cover_${Date.now()}.jpg`);
            const coverSnap = await uploadBytes(coverRef, coverBlob, { contentType: 'image/jpeg' });
            coverImageUrl = await getDownloadURL(coverSnap.ref);
          }
        }
      } else {
        coverImageUrl = fileUrl;
      }

      setUploadStatus('Creating extraction job...');
      const cleanTitle = file.name.replace(/\.[^/.]+$/, '').replace(/[-_]/g, ' ');

      const newFlyerDoc = await addFlyer({
        title: cleanTitle,
        retailerId: effectiveRetailerId,
        status: 'review_required',
        sourceType: 'merchant_upload',
        sourceAssetId: storagePath,
        pdfUrl: isPdf ? fileUrl : '',
        imageUrl: coverImageUrl || fileUrl,
        pages: pageCount,
        createdAt: Date.now(),
        updatedAt: Date.now(),
        validFrom: Date.now(),
        validTo: Date.now() + 14 * 86400000,
        views: 0,
        clicks: 0
      });

      try {
        const createJob = httpsCallable(functions, 'createFlyerExtractionJob');
        await createJob({
          flyerId: newFlyerDoc.id,
          retailerId: effectiveRetailerId,
          sourceAssetId: storagePath,
          totalPages: Math.min(50, pageCount)
        });
      } catch (jobErr) {
        console.warn('Backend extractionJob log skipped (offline/unconfigured):', jobErr);
      }

      showToast('Flyer uploaded! Starting automatic product extraction...', 'success');
      navigate(`/ocr/${newFlyerDoc.id}`);
    } catch (err: any) {
      console.error('Upload error:', err);
      showToast('Failed to process uploaded file: ' + err.message, 'error');
    } finally {
      setIsProcessingUpload(false);
      setUploadStatus('');
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  return (
    <div className="flex-1 flex flex-col h-full">
      {/* Hidden file input */}
      <input
        type="file"
        ref={fileInputRef}
        onChange={e => e.target.files?.[0] && handleFileSelected(e.target.files[0])}
        accept=".pdf,image/png,image/jpeg,image/webp"
        className="hidden"
      />

      {/* Top Header */}
      <div className="mb-6 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">OCR Extraction Center</h1>
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-indigo-100 text-indigo-700 uppercase tracking-wide">
              <Sparkles className="w-3 h-3" /> Auto-Capture
            </span>
          </div>
          <p className="text-slate-500 mt-1 text-sm">
            Upload any supermarket flyer PDF — products, offer prices, and cropped photos are captured automatically.
          </p>
        </div>

        <div className="flex items-center gap-2 px-3.5 py-2 border border-emerald-200 text-emerald-700 bg-emerald-50 rounded-xl font-semibold text-xs w-full sm:w-auto justify-center">
          <ShieldCheck className="w-4 h-4 text-emerald-600" />
          Server-Protected AI Extraction
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 flex-1 min-h-0">
        
        {/* Upload Column */}
        <div className="lg:col-span-1 flex flex-col min-h-0">
          <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 flex-1 flex flex-col">
            <h3 className="font-bold text-slate-900 mb-4 flex items-center gap-2">
              <ScanLine className="w-5 h-5 text-indigo-600" />
              New Flyer Upload
            </h3>

            <div className="space-y-4 flex-1 flex flex-col">
              {(isAdminUser || isModeratorTeam) && (
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1.5">Target Retailer</label>
                  <select
                    value={selectedRetailerId}
                    onChange={e => setSelectedRetailerId(e.target.value)}
                    className="w-full px-3.5 py-2 border border-slate-200 rounded-xl bg-slate-50 text-xs font-semibold text-slate-800 focus:ring-2 focus:ring-indigo-500 outline-none"
                  >
                    <option value="">Select retailer...</option>
                    {retailers.map((r: any) => (
                      <option key={r.id} value={r.id}>{r.name}</option>
                    ))}
                  </select>
                </div>
              )}
              <div
                onClick={() => !isProcessingUpload && fileInputRef.current?.click()}
                onDragOver={e => e.preventDefault()}
                onDrop={e => {
                  e.preventDefault();
                  if (e.dataTransfer.files?.[0]) handleFileSelected(e.dataTransfer.files[0]);
                }}
                className={`border-2 border-dashed rounded-2xl p-8 flex flex-col items-center justify-center text-center transition-all cursor-pointer flex-1 ${
                  isProcessingUpload
                    ? 'border-indigo-400 bg-indigo-50/50'
                    : 'border-indigo-200 bg-indigo-50/20 hover:bg-indigo-50/60 hover:border-indigo-400'
                }`}
              >
                {isProcessingUpload ? (
                  <div className="flex flex-col items-center">
                    <Activity className="w-9 h-9 text-indigo-600 animate-spin mb-3" />
                    <p className="text-sm font-bold text-slate-900">{uploadStatus}</p>
                    <p className="text-xs text-slate-500 mt-1">Please wait a moment...</p>
                  </div>
                ) : (
                  <>
                    <div className="w-14 h-14 bg-indigo-100 rounded-2xl flex items-center justify-center mb-3 text-indigo-600 shadow-xs">
                      <Upload className="w-7 h-7" />
                    </div>
                    <p className="text-sm font-bold text-slate-900">Drop flyer PDF or images here</p>
                    <p className="text-xs text-slate-500 mt-1">Supports PDF, JPG, PNG, WebP up to 25MB</p>
                    <button
                      type="button"
                      className="mt-4 px-4 py-2 bg-indigo-600 text-white rounded-xl text-xs font-bold uppercase tracking-wide hover:bg-indigo-700 shadow-xs"
                    >
                      Browse PDF / Files
                    </button>
                  </>
                )}
              </div>

              <div className="flex items-center gap-4 py-1">
                <div className="flex-1 h-px bg-slate-200"></div>
                <span className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">HOW IT WORKS</span>
                <div className="flex-1 h-px bg-slate-200"></div>
              </div>

              <div className="bg-slate-50 rounded-2xl p-4 border border-slate-100 space-y-2 text-xs text-slate-600">
                <div className="flex items-start gap-2">
                  <span className="w-4 h-4 rounded-full bg-indigo-100 text-indigo-700 font-bold flex items-center justify-center text-[10px] shrink-0 mt-0.5">1</span>
                  <span>Upload flyer PDF from merchant app</span>
                </div>
                <div className="flex items-start gap-2">
                  <span className="w-4 h-4 rounded-full bg-indigo-100 text-indigo-700 font-bold flex items-center justify-center text-[10px] shrink-0 mt-0.5">2</span>
                  <span>AI automatically crops each product image & extracts prices</span>
                </div>
                <div className="flex items-start gap-2">
                  <span className="w-4 h-4 rounded-full bg-indigo-100 text-indigo-700 font-bold flex items-center justify-center text-[10px] shrink-0 mt-0.5">3</span>
                  <span>Review & click "Publish" to sync live to consumer app</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Queue Column */}
        <div className="lg:col-span-2 flex flex-col min-h-0">
          <div className="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden flex flex-col flex-1">
            <div className="px-6 py-5 border-b border-slate-100 flex items-center justify-between shrink-0">
              <h3 className="font-bold text-slate-900">Review Queue (Pending Extractions)</h3>
              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold bg-slate-100 text-slate-800 uppercase tracking-wide">
                {pendingJobs.length} Jobs
              </span>
            </div>

            <div className="flex-1 overflow-y-auto w-full">
              {loading ? (
                <div className="p-12 text-center text-slate-500 font-medium animate-pulse flex items-center justify-center gap-2">
                  <Activity className="w-5 h-5 animate-spin text-indigo-600" /> Loading jobs...
                </div>
              ) : pendingJobs.length === 0 ? (
                <div className="p-12 text-center flex flex-col items-center justify-center h-full">
                  <FileText className="w-12 h-12 text-slate-300 mb-4" />
                  <h3 className="text-lg font-bold text-slate-900 mb-1">Queue is empty</h3>
                  <p className="text-sm text-slate-500">
                    Upload a flyer PDF on the left to start automated extraction.
                  </p>
                </div>
              ) : (
                pendingJobs.map(job => (
                  <div
                    key={job.id}
                    className="p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:bg-slate-50/80 transition-colors border-b border-slate-100 last:border-0 relative"
                  >
                    <div className="flex items-start gap-4">
                      {job.imageUrl ? (
                        <img
                          src={job.imageUrl}
                          alt={job.title}
                          className="w-16 h-20 object-cover rounded-xl border border-slate-200 bg-white shrink-0 shadow-2xs"
                        />
                      ) : (
                        <div className="w-16 h-20 bg-slate-100 rounded-xl flex items-center justify-center shrink-0 border border-slate-200">
                          <FileText className="w-6 h-6 text-slate-400" />
                        </div>
                      )}
                      <div>
                        <h4 className="font-bold text-sm text-slate-900">{job.title}</h4>
                        <div className="text-xs text-slate-500 mt-0.5">
                          {job.retailerId || 'Store'} • {job.pdfUrl ? 'PDF Document' : 'Flyer Image'}
                        </div>
                        <div className="mt-2 flex flex-wrap items-center gap-2">
                          <span className="text-[10px] font-bold text-amber-700 bg-amber-100 px-2 py-0.5 rounded uppercase tracking-wide">
                            Ready for Extraction & Review
                          </span>
                        </div>
                      </div>
                    </div>

                    <button
                      onClick={() => navigate(`/ocr/${job.id}`)}
                      className="sm:w-auto w-full flex items-center justify-center gap-1.5 px-4 py-2.5 bg-slate-900 text-white hover:bg-slate-800 rounded-xl transition-all font-bold text-xs uppercase tracking-wider shrink-0 shadow-xs"
                    >
                      Start Auto-Extraction <ArrowRight className="w-3.5 h-3.5" />
                    </button>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
