import React, { useState, useMemo } from 'react';
import { useCollection } from '../lib/firebase-hooks';
import { ShieldCheck, Search, Database, Clock, ChevronDown, ChevronLeft, ChevronRight } from 'lucide-react';

export default function AuditLogs() {
  const { data: logs, loading } = useCollection<any>('auditLogs');
  const [searchQuery, setSearchQuery] = useState('');
  const [filterAction, setFilterAction] = useState('ALL');
  const [expandedLogId, setExpandedLogId] = useState<string | null>(null);

  const [currentPage, setCurrentPage] = useState(1);
  const ITEMS_PER_PAGE = 25;

  const filteredLogs = useMemo(() => {
    return logs
      .map((l: any) => ({...l, timestamp: l.timestamp || 0}))
      .sort((a: any, b: any) => b.timestamp - a.timestamp) // Sort descending
      .filter((l: any) => {
        const matchesSearch = 
           l.user?.toLowerCase().includes(searchQuery.toLowerCase()) ||
           l.collection?.toLowerCase().includes(searchQuery.toLowerCase()) ||
           l.action?.toLowerCase().includes(searchQuery.toLowerCase()) ||
           l.documentId?.toLowerCase().includes(searchQuery.toLowerCase());
        
        const matchesAction = filterAction === 'ALL' || l.action === filterAction;
        
        return matchesSearch && matchesAction;
      });
  }, [logs, searchQuery, filterAction]);

  const paginatedLogs = useMemo(() => {
    const startIdx = (currentPage - 1) * ITEMS_PER_PAGE;
    return filteredLogs.slice(startIdx, startIdx + ITEMS_PER_PAGE);
  }, [filteredLogs, currentPage]);

  const totalPages = Math.ceil(filteredLogs.length / ITEMS_PER_PAGE);

  React.useEffect(() => {
    setCurrentPage(1);
  }, [searchQuery, filterAction]);

  const toggleExpand = (id: string) => {
    if (expandedLogId === id) setExpandedLogId(null);
    else setExpandedLogId(id);
  };

  const getActionColor = (action: string) => {
    switch (action) {
      case 'CREATE': return 'bg-blue-100 text-blue-700 border-blue-200';
      case 'UPDATE': return 'bg-slate-100 text-slate-700 border-slate-200';
      case 'DELETE': return 'bg-red-100 text-red-700 border-red-200';
      case 'APPROVE / PUBLISH': return 'bg-green-100 text-green-700 border-green-200';
      case 'REJECT': return 'bg-orange-100 text-orange-700 border-orange-200';
      case 'SUSPEND': return 'bg-red-100 text-red-700 border-red-200';
      default: return 'bg-slate-100 text-slate-700 border-slate-200';
    }
  };

  return (
    <div className="flex-1 flex flex-col h-full max-w-6xl mx-auto w-full">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <ShieldCheck className="w-6 h-6 text-indigo-600" /> System Audit Logs
          </h1>
          <p className="text-slate-500 mt-1 text-sm">Review all actions and data modifications across the system.</p>
        </div>
        <div className="flex items-center gap-3 w-full sm:w-auto">
           <select 
             value={filterAction}
             onChange={(e) => setFilterAction(e.target.value)}
             className="px-4 py-2 border border-slate-200 rounded-xl bg-white focus:ring-2 focus:ring-indigo-500 text-sm outline-none font-bold text-slate-700 shadow-sm"
           >
             <option value="ALL">All Actions</option>
             <option value="CREATE">CREATE</option>
             <option value="UPDATE">UPDATE</option>
             <option value="DELETE">DELETE</option>
             <option value="APPROVE / PUBLISH">APPROVE / PUBLISH</option>
             <option value="REJECT">REJECT</option>
             <option value="SUSPEND">SUSPEND</option>
           </select>
           <div className="relative w-full sm:w-64">
             <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 w-4 h-4" />
             <input 
               type="text" 
               value={searchQuery}
               onChange={(e) => setSearchQuery(e.target.value)}
               placeholder="Search logs..." 
               className="w-full pl-9 pr-4 py-2 border border-slate-200 rounded-xl bg-white focus:ring-2 focus:ring-indigo-500 text-sm outline-none shadow-sm"
             />
           </div>
        </div>
      </div>

      {loading ? (
        <div className="flex-1 flex items-center justify-center">
          <div className="text-slate-500 font-medium animate-pulse">Loading audit logs...</div>
        </div>
      ) : (
        <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden flex-1 overflow-y-auto">
          {filteredLogs.length === 0 ? (
            <div className="flex-1 flex flex-col items-center justify-center h-64 text-center">
              <Database className="w-12 h-12 text-slate-300 mb-4" />
              <h3 className="text-lg font-bold text-slate-900 mb-1">No logs found</h3>
              <p className="text-sm text-slate-500">No actions match your search criteria.</p>
            </div>
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50/80 border-b border-slate-200">
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">Timestamp / User</th>
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">Action</th>
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">Resource / Entity</th>
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider text-right">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                 {paginatedLogs.map((log: any) => (
                   <React.Fragment key={log.id}>
                     <tr className="hover:bg-slate-50 transition-colors cursor-pointer" onClick={() => toggleExpand(log.id)}>
                       <td className="py-4 px-6">
                          <div className="flex flex-col gap-0.5">
                             <div className="font-semibold text-slate-900 text-sm flex items-center gap-1">
                               <Clock className="w-3 h-3 text-slate-400" />
                               {log.timestamp ? new Date(log.timestamp).toLocaleString() : 'Unknown Time'}
                             </div>
                             <div className="text-xs text-slate-500">{log.user || 'system'}</div>
                          </div>
                       </td>
                       <td className="py-4 px-6">
                         <span className={`inline-flex items-center px-2 py-0.5 rounded border text-[10px] font-bold uppercase tracking-widest ${getActionColor(log.action)}`}>
                           {log.action}
                         </span>
                       </td>
                       <td className="py-4 px-6">
                           <div className="text-sm font-semibold text-slate-700 capitalize">{log.collection}</div>
                           <div className="text-xs text-slate-400 font-mono mt-0.5 truncate max-w-[200px]" title={log.documentId}>{log.documentId}</div>
                       </td>
                       <td className="py-4 px-6 text-right">
                          <button className="text-slate-400 hover:text-indigo-600 transition-colors">
                            <ChevronDown className={`w-5 h-5 transition-transform ${expandedLogId === log.id ? 'rotate-180 text-indigo-600' : ''}`} />
                          </button>
                       </td>
                     </tr>
                     {expandedLogId === log.id && (
                       <tr>
                         <td colSpan={4} className="px-6 py-4 bg-slate-50 border-b border-slate-200">
                            <div className="text-xs font-mono text-slate-700 whitespace-pre-wrap break-all bg-white p-4 rounded-xl border border-slate-200 overflow-x-auto shadow-inner">
                              {log.details || 'No additional details provided.'}
                            </div>
                         </td>
                       </tr>
                     )}
                   </React.Fragment>
                 ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {totalPages > 1 && !loading && filteredLogs.length > 0 && (
        <div className="px-6 py-4 border-t border-slate-200 bg-white rounded-b-2xl flex justify-between items-center shrink-0">
          <span className="text-xs text-slate-500 font-medium">
            Showing {(currentPage - 1) * ITEMS_PER_PAGE + 1} - {Math.min(currentPage * ITEMS_PER_PAGE, filteredLogs.length)} of {filteredLogs.length}
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
  );
}
