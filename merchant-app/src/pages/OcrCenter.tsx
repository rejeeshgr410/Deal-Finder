import { ScanLine, Upload, Camera, FileText, Settings, ArrowRight } from 'lucide-react';
import { useCollection } from '../lib/firebase-hooks';
import { useNavigate } from 'react-router-dom';

export default function OcrCenter() {
  const { data: flyers, loading } = useCollection<any>('flyers');
  const navigate = useNavigate();

  const pendingJobs = flyers.filter(f => f.status === 'processing' || f.status === 'ocr_review' || f.status === 'draft' || f.status === 'pending_review' || f.status === 'pending');

  return (
    <div className="flex-1 flex flex-col h-full">
      <div className="mb-6 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">OCR Extraction Center</h1>
          <p className="text-slate-500 mt-1 text-sm">Extract products, prices, and offers automatically from flyers.</p>
        </div>
        <button className="flex items-center gap-2 px-4 py-2 border border-slate-200 text-slate-700 bg-white rounded-xl hover:bg-slate-50 transition-colors font-semibold text-sm w-full sm:w-auto justify-center">
          <Settings className="w-4 h-4" />
          OCR Settings
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 flex-1">
        {/* Upload Column */}
        <div className="lg:col-span-1 flex flex-col min-h-0">
          <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 flex-1 flex flex-col">
            <h3 className="font-bold text-slate-900 mb-4 flex items-center gap-2">
              <ScanLine className="w-5 h-5 text-indigo-600" />
              New Extraction Job
            </h3>
            
            <div className="space-y-4 flex-1 flex flex-col">
              <div className="border-2 border-dashed border-indigo-200 rounded-2xl p-8 flex flex-col items-center justify-center text-center bg-indigo-50/30 hover:bg-indigo-50 transition-colors cursor-pointer flex-1">
                <Upload className="w-8 h-8 text-indigo-500 mb-3" />
                <p className="text-sm font-bold text-slate-900">Drop flyer PDF or images here</p>
                <p className="text-xs text-slate-500 mt-1">Supports PDF, JPG, PNG up to 50MB</p>
                <button className="mt-4 px-4 py-2 bg-indigo-100 text-indigo-700 rounded-lg text-xs font-bold uppercase tracking-wide hover:bg-indigo-200 transition-colors">
                  Browse Files
                </button>
              </div>

              <div className="flex items-center gap-4 py-2">
                <div className="flex-1 h-px bg-slate-200"></div>
                <span className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">OR</span>
                <div className="flex-1 h-px bg-slate-200"></div>
              </div>

              <button className="w-full flex items-center justify-center gap-2 px-4 py-3 border border-slate-200 text-slate-700 bg-white rounded-xl hover:bg-slate-50 transition-colors font-semibold text-sm shadow-sm">
                <Camera className="w-4 h-4" />
                Capture from Camera
              </button>
            </div>
          </div>
        </div>

        {/* Queue Column */}
        <div className="lg:col-span-2 flex flex-col min-h-0">
          <div className="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden flex flex-col flex-1">
            <div className="px-6 py-5 border-b border-slate-100 flex items-center justify-between shrink-0">
              <h3 className="font-bold text-slate-900">Review Queue (Pending)</h3>
              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold bg-slate-100 text-slate-800 uppercase tracking-wide">
                {pendingJobs.length} Jobs
              </span>
            </div>
            
            <div className="flex-1 overflow-y-auto w-full">
              {loading ? (
                <div className="p-12 text-center text-slate-500 font-medium animate-pulse">Loading jobs...</div>
              ) : pendingJobs.length === 0 ? (
                <div className="p-12 text-center flex flex-col items-center justify-center h-full">
                  <FileText className="w-12 h-12 text-slate-300 mb-4" />
                  <h3 className="text-lg font-bold text-slate-900 mb-1">Queue is empty</h3>
                  <p className="text-sm text-slate-500">All OCR extractions have been reviewed.</p>
                </div>
              ) : (
                pendingJobs.map((job) => (
                  <div key={job.id} className="p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:bg-slate-50 transition-colors border-b border-slate-50 last:border-0 relative">
                     <div className="flex items-start gap-4">
                       <div className="w-16 h-20 bg-slate-100 rounded-xl flex items-center justify-center shrink-0">
                         <FileText className="w-6 h-6 text-slate-400" />
                       </div>
                       <div>
                         <h4 className="font-semibold text-slate-900">{job.title}</h4>
                         <div className="text-xs text-slate-500 mt-1">
                           {job.retailerId || 'Unknown Retailer'} • Requires validation
                         </div>
                         <div className="mt-2 flex flex-wrap items-center gap-2">
                           <span className="text-[10px] font-bold text-orange-700 bg-orange-100 px-2 py-1 rounded uppercase tracking-wide">Needs Review</span>
                         </div>
                       </div>
                     </div>
                     <button onClick={() => navigate(`/ocr/${job.id}`)} className="sm:w-auto w-full flex items-center justify-center gap-2 px-4 py-2 bg-slate-900 text-white hover:bg-slate-800 rounded-xl transition-colors font-bold text-[10px] uppercase tracking-wider shrink-0">
                       Start Review <ArrowRight className="w-3 h-3" />
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
