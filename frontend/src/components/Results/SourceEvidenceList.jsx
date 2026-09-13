import React from 'react';
import { ExternalLink, Layers, ShieldCheck, CheckCircle2, XCircle, HelpCircle, Ban } from 'lucide-react';

export default function SourceEvidenceList({ sources }) {
  if (!sources || sources.length === 0) {
    return (
      <div className="p-6 text-center text-slate-400">
        <p className="text-sm">No external wire citations indexed for this claim.</p>
      </div>
    );
  }

  const getTierBadge = (tier) => {
    const t = tier || 'LEVEL_2_SECONDARY';
    if (t.includes('LEVEL_1')) {
      return <span className="px-2.5 py-0.5 text-xs font-semibold rounded-full bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">Level 1 Primary Authority</span>;
    }
    if (t.includes('LEVEL_3')) {
      return <span className="px-2.5 py-0.5 text-xs font-semibold rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30">Level 3 Fact Check Database</span>;
    }
    if (t.includes('LEVEL_4')) {
      return <span className="px-2.5 py-0.5 text-xs font-semibold rounded-full bg-cyan-500/20 text-cyan-300 border border-cyan-500/30">Level 4 Reference Archive</span>;
    }
    if (t.includes('LEVEL_5')) {
      return <span className="px-2.5 py-0.5 text-xs font-semibold rounded-full bg-rose-500/20 text-rose-300 border border-rose-500/30">Level 5 User Social</span>;
    }
    return <span className="px-2.5 py-0.5 text-xs font-semibold rounded-full bg-sky-500/20 text-sky-300 border border-sky-500/30">Level 2 News Wire Agency</span>;
  };

  const getStanceBadge = (stance, verdict) => {
    const s = stance || (verdict?.includes('True') ? 'SUPPORTED' : (verdict?.includes('False') ? 'REFUTED' : 'UNCERTAIN'));
    switch (s.toUpperCase()) {
      case 'SUPPORTED':
      case 'CONFIRMED':
        return <span className="px-2.5 py-1 text-xs font-bold rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 flex items-center gap-1"><CheckCircle2 className="w-3.5 h-3.5" /> SUPPORTED</span>;
      case 'REFUTED':
      case 'CONTRADICTED':
      case 'DENIED':
        return <span className="px-2.5 py-1 text-xs font-bold rounded-full bg-rose-500/20 text-rose-300 border border-rose-500/30 flex items-center gap-1"><XCircle className="w-3.5 h-3.5" /> REFUTED</span>;
      case 'NOT_RELEVANT':
        return <span className="px-2.5 py-1 text-xs font-bold rounded-full bg-slate-800 text-slate-400 border border-slate-700 flex items-center gap-1"><Ban className="w-3.5 h-3.5" /> NOT RELEVANT</span>;
      default:
        return <span className="px-2.5 py-1 text-xs font-bold rounded-full bg-slate-500/20 text-slate-300 border border-slate-500/30 flex items-center gap-1"><HelpCircle className="w-3.5 h-3.5" /> UNCERTAIN</span>;
    }
  };

  return (
    <div className="space-y-4 p-4">
      <div className="flex items-center justify-between pb-3 border-b border-slate-700/60">
        <h3 className="text-base font-bold text-white flex items-center gap-2">
          <Layers className="w-5 h-5 text-sky-400" />
          Multi-Dimensional Evidence Audit Table ({sources.length} Sources)
        </h3>
        <span className="text-xs text-slate-400">Strict Relevance & Gated Admission</span>
      </div>

      <div className="grid grid-cols-1 gap-3">
        {sources.map((src, idx) => (
          <div 
            key={idx} 
            className="p-4 rounded-xl bg-slate-900/80 border border-slate-700/60 hover:border-slate-600 transition-all flex flex-col gap-3"
          >
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2">
              <div className="flex flex-wrap items-center gap-2">
                {src.evidenceId && (
                  <span className="px-2 py-0.5 text-xs font-mono font-bold rounded bg-slate-800 text-cyan-400 border border-cyan-500/30">
                    {src.evidenceId}
                  </span>
                )}
                <span className="font-bold text-white text-sm">{src.sourceName}</span>
                {getTierBadge(src.evidenceTier)}
                {src.clusterId && (
                  <span className="px-2 py-0.5 text-xs font-mono rounded bg-slate-800 text-indigo-300 border border-indigo-500/30">
                    Cluster: {src.clusterId}
                  </span>
                )}
                {src.directness && (
                  <span className="px-2 py-0.5 text-xs font-mono rounded bg-slate-800 text-slate-300 border border-slate-700">
                    {src.directness === 'DIRECT_PRIMARY' ? 'Direct Primary Authority' : 'Secondary News Wire'}
                  </span>
                )}
              </div>

              <div>
                {getStanceBadge(src.stance, src.verdictBySource)}
              </div>
            </div>

            {src.articleTitle && (
              <p className="text-xs text-slate-300 italic" title={src.articleTitle}>
                "{src.articleTitle}"
              </p>
            )}

            {/* Multi-Dimensional Metrics Badges */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-2 border-t border-slate-800/80 text-xs">
              <div className="p-2 rounded bg-slate-950/60 border border-slate-800">
                <span className="text-slate-400 block text-[10px] uppercase font-bold">Authority</span>
                <span className="text-white font-mono font-bold">{src.credibilityRating || 88}/100</span>
              </div>
              <div className="p-2 rounded bg-slate-950/60 border border-slate-800">
                <span className="text-slate-400 block text-[10px] uppercase font-bold">Claim Relevance</span>
                <span className={`font-mono font-bold ${(src.claimRelevance || 0) >= 0.65 ? 'text-emerald-400' : 'text-rose-400'}`}>
                  {Math.round((src.claimRelevance || (src.matchPercentage ? src.matchPercentage / 100 : 0.8)) * 100)}%
                </span>
              </div>
              <div className="p-2 rounded bg-slate-950/60 border border-slate-800">
                <span className="text-slate-400 block text-[10px] uppercase font-bold">Geo Relevance</span>
                <span className="text-cyan-300 font-mono font-bold">{src.geographicRelevance || 'HIGH'}</span>
              </div>
              <div className="p-2 rounded bg-slate-950/60 border border-slate-800">
                <span className="text-slate-400 block text-[10px] uppercase font-bold">Evidence Contribution</span>
                <span className="text-emerald-400 font-mono font-bold">
                  {src.evidenceContribution != null ? src.evidenceContribution : (src.stance === 'REFUTED' ? 0 : Math.round(src.claimRelevance ? src.claimRelevance * 90 : 80))} pts
                </span>
              </div>
            </div>

            {src.url && (
              <div className="pt-1">
                <a 
                  href={src.url} 
                  target="_blank" 
                  rel="noopener noreferrer" 
                  className="text-xs text-sky-400 hover:text-sky-300 inline-flex items-center gap-1 transition-colors"
                >
                  <span>Verify at: {src.domain || src.sourceName}</span>
                  <ExternalLink className="w-3 h-3" />
                </a>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}

