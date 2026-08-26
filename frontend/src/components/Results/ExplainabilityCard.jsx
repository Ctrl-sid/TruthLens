import React from 'react';
import { 
  ShieldCheck, 
  AlertTriangle, 
  CheckCircle2, 
  HelpCircle, 
  ExternalLink, 
  Layers, 
  Split, 
  Scale, 
  FileText,
  AlertCircle
} from 'lucide-react';

export default function ExplainabilityCard({ result }) {
  if (!result) return null;

  const explainability = result.explainability || {};
  const subClaims = result.subClaims || [];
  const positiveChecklist = explainability.positiveChecklist || [];
  const warningChecklist = explainability.warningChecklist || [];
  const detectedDiffs = explainability.detectedDifferences || [];
  const matrix = explainability.evidenceMatrix || [];
  const confidence = result.confidence || explainability.confidenceLevel || 'MEDIUM';

  const getConfidenceBadge = (conf) => {
    switch (conf?.toUpperCase()) {
      case 'HIGH':
        return <span className="px-3 py-1 text-xs font-semibold rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 flex items-center gap-1.5"><ShieldCheck className="w-3.5 h-3.5" /> High Confidence</span>;
      case 'LOW':
        return <span className="px-3 py-1 text-xs font-semibold rounded-full bg-rose-500/20 text-rose-300 border border-rose-500/30 flex items-center gap-1.5"><AlertTriangle className="w-3.5 h-3.5" /> Low Confidence</span>;
      default:
        return <span className="px-3 py-1 text-xs font-semibold rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30 flex items-center gap-1.5"><HelpCircle className="w-3.5 h-3.5" /> Medium Confidence</span>;
    }
  };

  const getStanceBadge = (stance) => {
    switch (stance?.toUpperCase()) {
      case 'SUPPORTED':
      case 'CONFIRMED':
        return <span className="px-2 py-0.5 text-xs font-medium rounded bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">SUPPORTED</span>;
      case 'REFUTED':
      case 'CONTRADICTED':
      case 'DENIED':
        return <span className="px-2 py-0.5 text-xs font-medium rounded bg-rose-500/20 text-rose-300 border border-rose-500/30">REFUTED</span>;
      default:
        return <span className="px-2 py-0.5 text-xs font-medium rounded bg-slate-500/20 text-slate-300 border border-slate-500/30">UNCERTAIN</span>;
    }
  };

  const getTierBadge = (tier) => {
    const t = tier || 'LEVEL_2_SECONDARY';
    if (t.includes('LEVEL_1')) {
      return <span className="px-2 py-0.5 text-[11px] font-semibold rounded bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">Level 1 Primary Gov</span>;
    }
    if (t.includes('LEVEL_3')) {
      return <span className="px-2 py-0.5 text-[11px] font-semibold rounded bg-amber-500/20 text-amber-300 border border-amber-500/30">Level 3 Fact Check</span>;
    }
    if (t.includes('LEVEL_4')) {
      return <span className="px-2 py-0.5 text-[11px] font-semibold rounded bg-cyan-500/20 text-cyan-300 border border-cyan-500/30">Level 4 Reference</span>;
    }
    if (t.includes('LEVEL_5')) {
      return <span className="px-2 py-0.5 text-[11px] font-semibold rounded bg-rose-500/20 text-rose-300 border border-rose-500/30">Level 5 User Social</span>;
    }
    return <span className="px-2 py-0.5 text-[11px] font-semibold rounded bg-sky-500/20 text-sky-300 border border-sky-500/30">Level 2 News Wire</span>;
  };

  return (
    <div className="space-y-6">
      {/* 1. Header & Confidence Status */}
      <div className="p-5 rounded-2xl bg-gradient-to-r from-slate-900/90 via-slate-800/80 to-slate-900/90 border border-slate-700/60 shadow-xl backdrop-blur-md">
        <div className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b border-slate-700/50">
          <div>
            <span className="text-xs uppercase tracking-wider font-bold text-sky-400">TruthLens Explainability Layer</span>
            <h3 className="text-lg font-bold text-white mt-0.5 flex items-center gap-2">
              <Scale className="w-5 h-5 text-sky-400" />
              Evidence Synthesis & Reasoning Matrix
            </h3>
          </div>
          <div className="flex items-center gap-2">
            {getConfidenceBadge(confidence)}
          </div>
        </div>

        {/* 2. Structured "Why?" Checklist */}
        <div className="mt-4 grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="p-4 rounded-xl bg-slate-950/50 border border-emerald-500/20 space-y-2.5">
            <h4 className="text-xs font-bold uppercase tracking-wider text-emerald-400 flex items-center gap-1.5">
              <CheckCircle2 className="w-4 h-4" /> Corroboration Checklist
            </h4>
            {positiveChecklist.length > 0 ? (
              <ul className="space-y-2 text-sm text-slate-200">
                {positiveChecklist.map((item, idx) => (
                  <li key={idx} className="flex items-start gap-2">
                    <span className="text-emerald-400 font-bold mt-0.5">✓</span>
                    <span>{item}</span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-xs text-slate-400 italic">No positive independent confirmations found.</p>
            )}
          </div>

          <div className="p-4 rounded-xl bg-slate-950/50 border border-amber-500/20 space-y-2.5">
            <h4 className="text-xs font-bold uppercase tracking-wider text-amber-400 flex items-center gap-1.5">
              <AlertTriangle className="w-4 h-4" /> Discrepancies & Cautionary Flags
            </h4>
            {warningChecklist.length > 0 ? (
              <ul className="space-y-2 text-sm text-slate-200">
                {warningChecklist.map((item, idx) => (
                  <li key={idx} className="flex items-start gap-2">
                    <span className="text-amber-400 font-bold mt-0.5">⚠</span>
                    <span>{item}</span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-xs text-slate-400 italic">No significant factual or numerical discrepancies detected.</p>
            )}
          </div>
        </div>

        {/* 3. Detected Factual Differences / Distortions Callout */}
        {detectedDiffs.length > 0 && (
          <div className="mt-4 p-4 rounded-xl bg-rose-950/30 border border-rose-500/30 space-y-2">
            <h4 className="text-xs font-bold uppercase tracking-wider text-rose-400 flex items-center gap-1.5">
              <AlertCircle className="w-4 h-4" /> Detected Factual Distortions
            </h4>
            <div className="space-y-1.5 text-sm text-rose-200">
              {detectedDiffs.map((diff, idx) => (
                <div key={idx} className="flex items-start gap-2">
                  <span className="font-bold text-rose-400">→</span>
                  <span>{diff}</span>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* 4. Atomic Sub-Claims Decomposition Breakdown */}
      {subClaims.length > 1 && (
        <div className="p-5 rounded-2xl bg-slate-900/80 border border-slate-700/60 shadow-xl backdrop-blur-md space-y-3">
          <div className="flex items-center justify-between pb-2 border-b border-slate-700/50">
            <h4 className="text-sm font-bold text-white flex items-center gap-2">
              <Split className="w-4 h-4 text-purple-400" />
              Claim Decomposition & Atomic Verification ({subClaims.length} Sub-Claims)
            </h4>
            <span className="text-xs text-slate-400">Independently Evaluated Propositions</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 pt-1">
            {subClaims.map((sub, idx) => (
              <div 
                key={idx} 
                className={`p-3.5 rounded-xl border ${
                  sub.claimVerdict === 'VERIFIED' ? 'bg-emerald-950/20 border-emerald-500/30' :
                  sub.claimVerdict === 'REFUTED' ? 'bg-rose-950/20 border-rose-500/30' :
                  'bg-slate-950/40 border-slate-700/50'
                }`}
              >
                <div className="flex items-center justify-between gap-2 mb-1.5">
                  <span className="text-[11px] uppercase font-bold tracking-wider text-slate-400">
                    Claim #{idx + 1} • {sub.claimType?.replace('_', ' ')}
                  </span>
                  {getStanceBadge(sub.stance)}
                </div>
                <p className="text-sm font-medium text-white mb-2">"{sub.claimText}"</p>
                <div className="text-xs text-slate-300 flex items-center justify-between pt-2 border-t border-slate-800/80">
                  <span className="truncate pr-2">{sub.evidenceSummary}</span>
                  <span className="font-bold text-sky-400 shrink-0">{sub.claimScore}%</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 5. Evidence Quality & Hierarchy Matrix Table */}
      {matrix.length > 0 && (
        <div className="p-5 rounded-2xl bg-slate-900/80 border border-slate-700/60 shadow-xl backdrop-blur-md space-y-3">
          <div className="flex items-center justify-between pb-2 border-b border-slate-700/50">
            <h4 className="text-sm font-bold text-white flex items-center gap-2">
              <Layers className="w-4 h-4 text-sky-400" />
              5-Tier Evidence Quality & Stance Matrix
            </h4>
            <span className="text-xs text-slate-400">Hierarchical Evidence Evaluation</span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="bg-slate-950/60 text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-700/50">
                <tr>
                  <th className="py-2.5 px-3">Evidence Source</th>
                  <th className="py-2.5 px-3">Hierarchy Tier</th>
                  <th className="py-2.5 px-3">Factual Stance</th>
                  <th className="py-2.5 px-3">Reliability</th>
                  <th className="py-2.5 px-3">Independence</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/70">
                {matrix.map((item, idx) => (
                  <tr key={idx} className="hover:bg-slate-800/40 transition-colors">
                    <td className="py-2.5 px-3 font-semibold text-white">{item.sourceName}</td>
                    <td className="py-2.5 px-3">{getTierBadge(item.evidenceTier)}</td>
                    <td className="py-2.5 px-3">{getStanceBadge(item.stance)}</td>
                    <td className="py-2.5 px-3">
                      <span className={`font-semibold ${item.reliability === 'HIGH' ? 'text-emerald-400' : 'text-amber-400'}`}>
                        {item.reliability}
                      </span>
                    </td>
                    <td className="py-2.5 px-3">
                      <span className="font-mono text-sky-400 font-bold">{Math.round(item.independence)}%</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
