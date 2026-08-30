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
  AlertCircle,
  Radio,
  Clock,
  MapPin,
  Tag,
  Quote,
  Activity,
  Globe,
  Building2,
  Check,
  X,
  Search
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
  const confidenceScore = result.confidenceScore || explainability.confidenceScore || 75;
  const completeness = result.evidenceCompleteness != null ? result.evidenceCompleteness : (explainability.evidenceCompleteness || 85);
  const severity = result.contradictionSeverity || 'NONE';
  const context = result.claimContext || {};
  const audit = result.retrievalAudit || explainability.retrievalAudit || {};

  const getConfidenceBadge = (conf, score) => {
    switch (conf?.toUpperCase()) {
      case 'HIGH':
        return (
          <span className="px-3 py-1 text-xs font-semibold rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 flex items-center gap-1.5">
            <ShieldCheck className="w-3.5 h-3.5" /> High Confidence ({score}%)
          </span>
        );
      case 'LOW':
        return (
          <span className="px-3 py-1 text-xs font-semibold rounded-full bg-rose-500/20 text-rose-300 border border-rose-500/30 flex items-center gap-1.5">
            <AlertTriangle className="w-3.5 h-3.5" /> Low Confidence ({score}%)
          </span>
        );
      default:
        return (
          <span className="px-3 py-1 text-xs font-semibold rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30 flex items-center gap-1.5">
            <HelpCircle className="w-3.5 h-3.5" /> Medium Confidence ({score}%)
          </span>
        );
    }
  };

  const getStanceBadge = (stance) => {
    switch (stance?.toUpperCase()) {
      case 'CONFIRMED':
        return <span className="px-2 py-0.5 text-[11px] font-bold rounded bg-emerald-500/25 text-emerald-300 border border-emerald-500/40">CONFIRMED</span>;
      case 'SUPPORTED':
        return <span className="px-2 py-0.5 text-[11px] font-medium rounded bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">SUPPORTED</span>;
      case 'ARTICLE_REPORTS_CLAIM':
        return <span className="px-2 py-0.5 text-[11px] font-medium rounded bg-sky-500/20 text-sky-300 border border-sky-500/30">REPORTS CLAIM</span>;
      case 'PARTIALLY_SUPPORTED':
      case 'DEVELOPING':
        return <span className="px-2 py-0.5 text-[11px] font-medium rounded bg-amber-500/20 text-amber-300 border border-amber-500/30">DEVELOPING</span>;
      case 'NOT_MENTIONED':
        return <span className="px-2 py-0.5 text-[11px] font-medium rounded bg-slate-500/20 text-slate-400 border border-slate-600/30">NOT MENTIONED</span>;
      case 'DENIED':
      case 'REFUTED':
      case 'CONTRADICTED':
        return <span className="px-2 py-0.5 text-[11px] font-bold rounded bg-rose-500/20 text-rose-300 border border-rose-500/30">REFUTED</span>;
      default:
        return <span className="px-2 py-0.5 text-[11px] font-medium rounded bg-slate-500/20 text-slate-300 border border-slate-500/30">UNCERTAIN</span>;
    }
  };

  const getCentralityBadge = (centrality, weight) => {
    switch (centrality) {
      case 'PRIMARY_CLAIM':
        return <span className="px-2 py-0.5 text-[10px] font-bold uppercase rounded bg-purple-500/20 text-purple-300 border border-purple-500/30">Primary Core Fact</span>;
      case 'SUPPORTING_CLAIM':
        return <span className="px-2 py-0.5 text-[10px] font-medium uppercase rounded bg-sky-500/20 text-sky-300 border border-sky-500/30">Supporting Detail</span>;
      default:
        return <span className="px-2 py-0.5 text-[10px] font-medium uppercase rounded bg-slate-500/20 text-slate-400 border border-slate-600/30">Contextual Fact</span>;
    }
  };

  const getSeverityBadge = (sev) => {
    if (!sev || sev === 'NONE') return null;
    switch (sev) {
      case 'MINOR_DISCREPANCY':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30">Minor Variance (&lt;5%)</span>;
      case 'MODERATE_CONTRADICTION':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-orange-500/20 text-orange-300 border border-orange-500/30">Moderate Contradiction</span>;
      case 'MAJOR_CONTRADICTION':
      case 'DIRECT_FACTUAL_REVERSAL':
        return <span className="px-2.5 py-1 text-xs font-bold rounded-full bg-rose-500/25 text-rose-300 border border-rose-500/40">Direct Factual Reversal</span>;
      default:
        return null;
    }
  };

  const getTierBadge = (tier) => {
    const t = tier || 'LEVEL_2_SECONDARY';
    if (t.includes('LEVEL_1')) {
      return <span className="px-2 py-0.5 text-[11px] font-semibold rounded bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">Level 1 Primary Gov/Police</span>;
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
      {/* 1. Header, Confidence & Completeness Bar */}
      <div className="p-5 rounded-2xl bg-gradient-to-r from-slate-900/90 via-slate-800/80 to-slate-900/90 border border-slate-700/60 shadow-xl backdrop-blur-md">
        <div className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b border-slate-700/50">
          <div>
            <span className="text-xs uppercase tracking-wider font-bold text-sky-400">TruthLens Claim-Contextual Decision Layer</span>
            <h3 className="text-lg font-bold text-white mt-0.5 flex items-center gap-2">
              <Scale className="w-5 h-5 text-sky-400" />
              Evidence Synthesis & Explainability Report
            </h3>
          </div>
          <div className="flex items-center gap-2">
            {getSeverityBadge(severity)}
            {getConfidenceBadge(confidence, confidenceScore)}
          </div>
        </div>

        {/* Evidence Completeness & Context Info */}
        <div className="mt-4 grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-700/50 space-y-2">
            <div className="flex items-center justify-between text-xs font-bold uppercase tracking-wider">
              <span className="text-slate-300 flex items-center gap-1.5">
                <Activity className="w-4 h-4 text-sky-400" />
                Evidence Completeness
              </span>
              <span className="text-sky-400 font-mono font-bold text-sm">{completeness}% Verified</span>
            </div>
            <div className="w-full bg-slate-800 rounded-full h-2.5 overflow-hidden">
              <div 
                className="bg-gradient-to-r from-sky-500 to-emerald-400 h-2.5 rounded-full transition-all duration-500" 
                style={{ width: `${Math.max(5, completeness)}%` }}
              />
            </div>
            <p className="text-[11px] text-slate-400 leading-snug">
              {subClaims.length > 1 ? 
                `${subClaims.filter(s => s.claimVerdict === 'VERIFIED' || s.claimVerdict === 'MOSTLY_VERIFIED').length} of ${subClaims.length} atomic factual components independently substantiated.` :
                "Atomic factual proposition verified against authoritative press archives."}
            </p>
          </div>

          {context.domain && (
            <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-700/50 space-y-1.5">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
                <Globe className="w-4 h-4 text-emerald-400" />
                Claim Context Engine
              </span>
              <div className="text-xs text-slate-200">
                <span className="text-slate-400">Domain:</span> <span className="font-semibold text-white">{context.domain}</span>
              </div>
              {context.geographicEntities && context.geographicEntities.length > 0 && (
                <div className="text-xs text-slate-200">
                  <span className="text-slate-400">Regions:</span> <span className="font-semibold text-sky-300">{context.geographicEntities.join(", ")}</span>
                </div>
              )}
              {context.targetAuthorityInstitutions && context.targetAuthorityInstitutions.length > 0 && (
                <div className="text-[11px] text-slate-400 truncate">
                  <span className="text-slate-500">Target Authorities:</span> {context.targetAuthorityInstitutions.join(" • ")}
                </div>
              )}
            </div>
          )}
        </div>

        {/* 2. Structured Corroboration & Discrepancy Checklists */}
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

      {/* 4. Evidence Retrieval Audit Trail */}
      {audit.sourcesRetrieved > 0 && (
        <div className="p-5 rounded-2xl bg-slate-900/80 border border-slate-700/60 shadow-xl backdrop-blur-md space-y-3">
          <div className="flex items-center justify-between pb-2 border-b border-slate-700/50">
            <h4 className="text-sm font-bold text-white flex items-center gap-2">
              <Search className="w-4 h-4 text-sky-400" />
              Evidence Retrieval & Syndication Audit
            </h4>
            <span className="text-xs text-slate-400">Transparency & Candidate Audit</span>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-1">
            <div className="p-3 rounded-lg bg-slate-950/60 border border-slate-800 text-center">
              <span className="text-[11px] uppercase font-bold text-slate-400 block mb-0.5">Retrieved</span>
              <span className="text-lg font-mono font-bold text-white">{audit.sourcesRetrieved}</span>
              <span className="text-[10px] text-slate-500 block">Candidate articles</span>
            </div>
            <div className="p-3 rounded-lg bg-slate-950/60 border border-slate-800 text-center">
              <span className="text-[11px] uppercase font-bold text-slate-400 block mb-0.5">Syndicated</span>
              <span className="text-lg font-mono font-bold text-amber-400">{audit.syndicatedDuplicates}</span>
              <span className="text-[10px] text-slate-500 block">Republished copies</span>
            </div>
            <div className="p-3 rounded-lg bg-slate-950/60 border border-slate-800 text-center">
              <span className="text-[11px] uppercase font-bold text-slate-400 block mb-0.5">Clusters</span>
              <span className="text-lg font-mono font-bold text-sky-400">{audit.independentClustersCount}</span>
              <span className="text-[10px] text-slate-500 block">Independent wires</span>
            </div>
            <div className="p-3 rounded-lg bg-slate-950/60 border border-slate-800 text-center">
              <span className="text-[11px] uppercase font-bold text-slate-400 block mb-0.5">Primary/Accredited</span>
              <span className="text-lg font-mono font-bold text-emerald-400">{audit.relevantSources}</span>
              <span className="text-[10px] text-slate-500 block">Authoritative sources</span>
            </div>
          </div>
          {audit.auditSummary && (
            <p className="text-xs text-slate-400 italic pt-1">{audit.auditSummary}</p>
          )}
        </div>
      )}

      {/* 5. Atomic Sub-Claims Decomposition Breakdown */}
      {subClaims.length > 1 && (
        <div className="p-5 rounded-2xl bg-slate-900/80 border border-slate-700/60 shadow-xl backdrop-blur-md space-y-3">
          <div className="flex items-center justify-between pb-2 border-b border-slate-700/50">
            <h4 className="text-sm font-bold text-white flex items-center gap-2">
              <Split className="w-4 h-4 text-purple-400" />
              Claim Decomposition & Metric Normalization ({subClaims.length} Sub-Claims)
            </h4>
            <span className="text-xs text-slate-400">Contextual Sub-Claim Matrix</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 pt-1">
            {subClaims.map((sub, idx) => (
              <div 
                key={idx} 
                className={`p-3.5 rounded-xl border ${
                  sub.claimVerdict === 'VERIFIED' ? 'bg-emerald-950/20 border-emerald-500/30' :
                  sub.claimVerdict === 'MOSTLY_VERIFIED' ? 'bg-emerald-950/15 border-emerald-500/25' :
                  sub.claimVerdict === 'REFUTED' ? 'bg-rose-950/20 border-rose-500/30' :
                  'bg-slate-950/40 border-slate-700/50'
                }`}
              >
                <div className="flex items-center justify-between gap-2 mb-1.5">
                  <div className="flex items-center gap-1.5">
                    {getCentralityBadge(sub.claimCentrality, sub.claimImportanceWeight)}
                    <span className="text-[11px] text-slate-400 font-mono">#{idx + 1}</span>
                  </div>
                  {getStanceBadge(sub.stance)}
                </div>
                <p className="text-sm font-medium text-white mb-2">"{sub.claimText}"</p>
                {sub.targetMetric && (
                  <div className="mb-2 text-[11px] font-mono px-2 py-0.5 rounded bg-slate-900 border border-slate-800 text-sky-300 inline-block">
                    {sub.targetMetric}
                  </div>
                )}
                <div className="text-xs text-slate-300 flex items-center justify-between pt-2 border-t border-slate-800/80">
                  <span className="truncate pr-2">{sub.evidenceSummary}</span>
                  <span className="font-bold text-sky-400 shrink-0">{sub.claimScore != null ? `${sub.claimScore}%` : 'N/A'}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 6. Evidence Quality & Hierarchy Matrix Table with Justifications */}
      {matrix.length > 0 && (
        <div className="p-5 rounded-2xl bg-slate-900/80 border border-slate-700/60 shadow-xl backdrop-blur-md space-y-3">
          <div className="flex items-center justify-between pb-2 border-b border-slate-700/50">
            <h4 className="text-sm font-bold text-white flex items-center gap-2">
              <Layers className="w-4 h-4 text-sky-400" />
              Contextual Evidence Authority & Evaluation Matrix
            </h4>
            <span className="text-xs text-slate-400">Claim-Contextual Evaluation</span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="bg-slate-950/60 text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-700/50">
                <tr>
                  <th className="py-2.5 px-3">Evidence Source</th>
                  <th className="py-2.5 px-3">Authority Tier</th>
                  <th className="py-2.5 px-3">Factual Stance</th>
                  <th className="py-2.5 px-3">Directness</th>
                  <th className="py-2.5 px-3">Acceptance Justification</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/70">
                {matrix.map((item, idx) => (
                  <tr key={idx} className="hover:bg-slate-800/40 transition-colors">
                    <td className="py-2.5 px-3 font-semibold text-white">
                      <div>{item.sourceName}</div>
                      {item.geographicRelevance && (
                        <span className="text-[10px] text-slate-500">Geo: {item.geographicRelevance}</span>
                      )}
                    </td>
                    <td className="py-2.5 px-3">{getTierBadge(item.evidenceTier)}</td>
                    <td className="py-2.5 px-3">{getStanceBadge(item.stance)}</td>
                    <td className="py-2.5 px-3">
                      <span className="font-medium text-slate-300">
                        {item.directness ? item.directness.replace(/_/g, ' ') : 'Secondary'}
                      </span>
                    </td>
                    <td className="py-2.5 px-3 max-w-xs text-slate-400">
                      {item.acceptanceReasons && item.acceptanceReasons.length > 0 ? (
                        <div className="space-y-0.5">
                          {item.acceptanceReasons.map((r, i) => (
                            <div key={i} className="text-[11px] text-slate-300 flex items-center gap-1">
                              <Check className="w-3 h-3 text-emerald-400 shrink-0" />
                              <span className="truncate">{r}</span>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <span className="italic text-slate-500">Accredited press candidate</span>
                      )}
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
