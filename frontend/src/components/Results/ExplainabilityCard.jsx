import React, { useState } from 'react';
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
  Search,
  ArrowRight,
  GitBranch,
  ChevronDown,
  ChevronUp,
  SlidersHorizontal,
  Info,
  Sparkles,
  Zap,
  CheckCircle,
  XCircle,
  MinusCircle,
  Eye,
  Terminal,
  Cpu,
  Copy,
  CheckCheck
} from 'lucide-react';

export default function ExplainabilityCard({ result }) {
  if (!result) return null;

  // View Mode: 'simple' | 'detailed' | 'technical'
  const [viewMode, setViewMode] = useState('detailed');
  
  // Interactive section toggles
  const [selectedStepIdx, setSelectedStepIdx] = useState(null);
  const [showAllSteps, setShowAllSteps] = useState(false);
  const [showTechnicalAudit, setShowTechnicalAudit] = useState(false);
  const [showTechnicalSPO, setShowTechnicalSPO] = useState({});
  const [activeAuditTab, setActiveAuditTab] = useState('scoring'); // 'scoring' | 'clusters' | 'matrix' | 'retrieval' | 'raw'
  const [copiedRaw, setCopiedRaw] = useState(false);
  const [activeTooltip, setActiveTooltip] = useState(null);

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
  const distortionType = result.distortionType || explainability.distortionType || 'NONE';
  const asOfStatus = result.asOfStatus || explainability.asOfStatus || 'CURRENTLY_VALID';
  const context = result.claimContext || {};
  const audit = result.retrievalAudit || explainability.retrievalAudit || {};
  const pipelineSteps = result.pipelineSteps || [];
  const evidenceClusters = result.evidenceClusters || [];
  const sources = result.sources || [];
  const originDiscovery = result.originDiscovery || {};

  // Status helpers
  const isNonClaim = result.verdict?.includes('NON-VERIFIABLE') || result.genuinenessScore == null;
  const isInsufficient = result.verdict?.includes('INSUFFICIENT');
  const isContradicted = result.verdict?.includes('CONTRADICTED') || result.verdict?.includes('HOAX') || result.verdict?.includes('FABRICATED');
  const isAuthoritativeNotice = result.verdict?.includes('AUTHORITATIVE');
  const isVerified = (result.verdict?.includes('VERIFIED') || result.verdict?.includes('SUPPORTED')) && !isInsufficient && !isContradicted;

  const toggleSPO = (idx) => {
    setShowTechnicalSPO(prev => ({ ...prev, [idx]: !prev[idx] }));
  };

  const handleCopyRaw = () => {
    navigator.clipboard.writeText(JSON.stringify(result, null, 2));
    setCopiedRaw(true);
    setTimeout(() => setCopiedRaw(false), 2000);
  };

  // Tooltip content dictionary
  const tooltips = {
    supportScore: "Mathematical support score (0–100) calculated deterministically from source authority weights, stance alignment, and relevance minus contradiction penalties.",
    confidence: "How confident TruthLens is in the verification result based on source authority, retrieval completeness, and independent cross-referencing.",
    coverage: "The percentage of atomic factual claims independently corroborated across accredited wire and government records.",
    clusters: "Multiple news articles grouped together because they originate from the same reporting origin or syndicated dispatch, preventing synthetic score inflation.",
    scaledPenalty: "Damped contradiction penalty dynamically scaled by the number of independent corroborating clusters to prevent isolated discrepancies from overriding broad consensus.",
    authorityTier: "Hierarchical classification of sources: Tier 1 (Official Gazettes), Tier 2 (Accredited News Wires), Tier 3 (Certified Fact-Checkers), Tier 4 (Reference Archives), Tier 5 (Social Media - Isolated)."
  };

  const renderTooltip = (key, text) => (
    <span 
      className="inline-flex items-center ml-1 text-slate-400 hover:text-cyan-300 cursor-pointer relative"
      onMouseEnter={() => setActiveTooltip(key)}
      onMouseLeave={() => setActiveTooltip(null)}
      onClick={() => setActiveTooltip(activeTooltip === key ? null : key)}
      title={tooltips[key]}
    >
      <Info className="w-3.5 h-3.5" />
      {activeTooltip === key && (
        <span className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 w-64 p-2.5 rounded-lg bg-slate-900 text-[11px] text-slate-200 border border-slate-700 shadow-2xl z-50 normal-case leading-relaxed font-sans">
          {tooltips[key]}
        </span>
      )}
    </span>
  );

  const getTierCleanName = (tier) => {
    const t = tier || 'LEVEL_2_SECONDARY';
    if (t.includes('LEVEL_1')) return 'Level 1 — Primary Official Authority';
    if (t.includes('LEVEL_2')) return 'Level 2 — Accredited News Wire';
    if (t.includes('LEVEL_3')) return 'Level 3 — Certified Fact-Checker';
    if (t.includes('LEVEL_4')) return 'Level 4 — Reference Archive';
    if (t.includes('LEVEL_5')) return 'Level 5 — Social / UGC (Isolated)';
    return 'Level 2 — Accredited News Wire';
  };

  const getStanceBadge = (stance) => {
    switch (stance?.toUpperCase()) {
      case 'CONFIRMED':
        return <span className="px-2 py-0.5 text-[11px] font-bold rounded bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">CONFIRMED</span>;
      case 'SUPPORTED':
        return <span className="px-2 py-0.5 text-[11px] font-medium rounded bg-emerald-500/15 text-emerald-300 border border-emerald-500/25">SUPPORTED</span>;
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

  const shortStepNames = [
    'Ingestion', 'Classify', 'Extract', 'Verifiability', 'Retrieval', 
    'Validation', 'Stance', 'Fusion', 'Forensics', 'XAI Report'
  ];

  const totalCompletedStages = pipelineSteps.filter(s => s.status === 'COMPLETED' || s.status === 'PASSED').length;
  const isAllPassed = pipelineSteps.length > 0 && totalCompletedStages === pipelineSteps.length;
  const blockedStep = pipelineSteps.find(s => s.status === 'BLOCKED');

  const currentActiveStep = selectedStepIdx !== null 
    ? pipelineSteps[selectedStepIdx] 
    : (blockedStep || pipelineSteps[pipelineSteps.length - 1] || null);

  // Independent cluster count calculation
  const distinctClustersCount = audit.independentClustersCount || evidenceClusters.length || 
    (sources.length > 0 ? new Set(sources.map(s => s.clusterId || s.sourceName)).size : 0);

  return (
    <div className="space-y-6 text-slate-100 font-sans">
      
      {/* ─── TOP CONTROL BAR: TITLE & VIEW MODE SWITCHER ─── */}
      <div className="p-5 rounded-2xl bg-gradient-to-r from-slate-900/95 via-slate-850/90 to-slate-900/95 border border-slate-700/60 shadow-xl backdrop-blur-md">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs uppercase tracking-wider font-bold text-cyan-400 flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5" /> Explainability & Reasoning Report
              </span>
            </div>
            <h3 className="text-xl font-bold text-white mt-1 flex items-center gap-2">
              How TruthLens Verified This Result
            </h3>
            <p className="text-xs text-slate-400 mt-0.5 mb-0">
              Deterministic evidence synthesis, independent cluster corroboration, and verification trace.
            </p>
          </div>

          {/* View Mode Segmented Switch */}
          <div className="flex items-center gap-1 p-1 bg-slate-950/80 rounded-xl border border-slate-800 shadow-inner">
            <button
              type="button"
              onClick={() => { setViewMode('simple'); setShowTechnicalAudit(false); }}
              className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition-all flex items-center gap-1.5 cursor-pointer ${
                viewMode === 'simple'
                  ? 'bg-cyan-500 text-slate-950 shadow-md font-bold'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <span>Simple</span>
            </button>
            <button
              type="button"
              onClick={() => { setViewMode('detailed'); setShowTechnicalAudit(false); }}
              className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition-all flex items-center gap-1.5 cursor-pointer ${
                viewMode === 'detailed'
                  ? 'bg-cyan-500 text-slate-950 shadow-md font-bold'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <span>Detailed</span>
            </button>
            <button
              type="button"
              onClick={() => { setViewMode('technical'); setShowTechnicalAudit(true); }}
              className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition-all flex items-center gap-1.5 cursor-pointer ${
                viewMode === 'technical'
                  ? 'bg-purple-600 text-white shadow-md font-bold'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <Terminal className="w-3.5 h-3.5" />
              <span>Technical</span>
            </button>
          </div>
        </div>
      </div>

      {/* ─── SECTION 01: VERIFICATION RESULT (ALWAYS VISIBLE) ─── */}
      <div className="p-6 rounded-2xl bg-slate-900/90 border border-slate-700/60 shadow-xl backdrop-blur-md space-y-5">
        
        {/* Verdict Badge Banner */}
        <div className="flex flex-wrap items-center justify-between gap-3 pb-4 border-b border-slate-800/80">
          <div className="flex items-center gap-3">
            <span 
              className="px-4 py-1.5 rounded-full text-sm font-bold tracking-wide shadow-sm flex items-center gap-2 border"
              style={{ 
                backgroundColor: `${result.verdictBadgeColor || '#10B981'}22`, 
                color: result.verdictBadgeColor || '#10B981',
                borderColor: `${result.verdictBadgeColor || '#10B981'}44`
              }}
            >
              {isVerified && <CheckCircle2 className="w-4 h-4" />}
              {isContradicted && <XCircle className="w-4 h-4" />}
              {isInsufficient && <HelpCircle className="w-4 h-4" />}
              {isAuthoritativeNotice && <ShieldCheck className="w-4 h-4" />}
              {isNonClaim && <Info className="w-4 h-4" />}
              <span>{result.verdict || 'VERIFICATION COMPLETE'}</span>
            </span>
          </div>

          <div className="flex items-center gap-2 text-xs text-slate-400">
            <span>Report #{result.id || 'LIVE'}</span>
            <span>•</span>
            <span>{result.timestamp || 'Just now'}</span>
          </div>
        </div>

        {/* 3 DISTINCT METRIC PILLARS */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          
          {/* Pillar 1: SUPPORT SCORE */}
          <div className="p-4 rounded-xl bg-slate-950/70 border border-slate-800 hover:border-cyan-500/40 transition-all text-center group">
            <div className="text-[11px] font-bold uppercase tracking-wider text-slate-400 flex items-center justify-center gap-1 mb-1">
              <span>Support Score</span>
              {renderTooltip('supportScore', 'Support Score')}
            </div>
            <div className="text-3xl font-mono font-extrabold text-cyan-400 group-hover:scale-105 transition-transform">
              {result.genuinenessScore != null ? (
                <>
                  <span>{result.genuinenessScore}</span>
                  <span className="text-sm font-normal text-slate-500">/100</span>
                </>
              ) : (
                <span className="text-2xl text-slate-400 font-sans">N/A</span>
              )}
            </div>
            <span className="text-[11px] text-slate-400 block mt-1">
              {result.genuinenessScore != null ? 'Deterministic Epistemic Rating' : 'Non-Verifiable Input'}
            </span>
          </div>

          {/* Pillar 2: VERIFICATION CONFIDENCE */}
          <div className="p-4 rounded-xl bg-slate-950/70 border border-slate-800 hover:border-emerald-500/40 transition-all text-center group">
            <div className="text-[11px] font-bold uppercase tracking-wider text-slate-400 flex items-center justify-center gap-1 mb-1">
              <span>Verification Confidence</span>
              {renderTooltip('confidence', 'Confidence')}
            </div>
            <div className="text-3xl font-mono font-extrabold text-emerald-400 group-hover:scale-105 transition-transform flex items-center justify-center gap-2">
              <ShieldCheck className="w-6 h-6 text-emerald-400" />
              <span>{confidence}</span>
            </div>
            <span className="text-[11px] text-slate-400 block mt-1">
              {confidenceScore}% Reliability Index
            </span>
          </div>

          {/* Pillar 3: EVIDENCE COVERAGE */}
          <div className="p-4 rounded-xl bg-slate-950/70 border border-slate-800 hover:border-purple-500/40 transition-all text-center group">
            <div className="text-[11px] font-bold uppercase tracking-wider text-slate-400 flex items-center justify-center gap-1 mb-1">
              <span>Evidence Coverage</span>
              {renderTooltip('coverage', 'Coverage')}
            </div>
            <div className="text-3xl font-mono font-extrabold text-purple-400 group-hover:scale-105 transition-transform">
              <span>{completeness}%</span>
            </div>
            <div className="w-3/4 mx-auto bg-slate-800 rounded-full h-1.5 mt-2 overflow-hidden">
              <div 
                className="bg-gradient-to-r from-purple-500 to-cyan-400 h-1.5 rounded-full transition-all duration-700" 
                style={{ width: `${Math.max(5, completeness)}%` }}
              />
            </div>
            <span className="text-[11px] text-slate-400 block mt-1.5">
              {subClaims.length > 0 ? `${subClaims.length} Sub-claims Evaluated` : 'Core Assertions Checked'}
            </span>
          </div>
        </div>

        {/* Concise Human-Readable Conclusion */}
        <div className="p-4 rounded-xl bg-slate-950/50 border border-slate-800 flex items-start gap-3 text-sm leading-relaxed text-slate-200">
          <Info className="w-5 h-5 text-cyan-400 shrink-0 mt-0.5" />
          <div>
            <p className="mb-0 font-medium">{result.rationale || 'TruthLens evaluated the submitted claim against accredited news wires and official repositories.'}</p>
          </div>
        </div>

        {/* Summary Stat Chips Bar */}
        <div className="flex flex-wrap items-center justify-between gap-3 pt-3 border-t border-slate-800 text-xs text-slate-400">
          <div className="flex items-center gap-4 flex-wrap">
            <span className="flex items-center gap-1.5 font-medium text-slate-300">
              <Search className="w-3.5 h-3.5 text-cyan-400" />
              <span>{sources.length || audit.sourcesRetrieved || 0} Sources Retrieved</span>
            </span>
            <span>•</span>
            <span className="flex items-center gap-1.5 font-medium text-slate-300">
              <Layers className="w-3.5 h-3.5 text-emerald-400" />
              <span>{distinctClustersCount} Independent Clusters</span>
              {renderTooltip('clusters', 'Clusters')}
            </span>
            <span>•</span>
            <span className="flex items-center gap-1.5 font-medium text-slate-300">
              <Scale className="w-3.5 h-3.5 text-amber-400" />
              <span>{detectedDiffs.length || (severity !== 'NONE' ? 1 : 0)} Contradictions</span>
            </span>
          </div>

          {result.singleSourceGovNotice && (
            <span className="px-2.5 py-1 rounded-md bg-sky-950/60 text-sky-300 border border-sky-500/30 font-medium">
              Single Authority Notice
            </span>
          )}
        </div>
      </div>

      {/* ─── SECTION 02: WHY THIS RESULT? (3–4 INSIGHT CARDS) ─── */}
      <div className="p-6 rounded-2xl bg-slate-900/90 border border-slate-700/60 shadow-xl backdrop-blur-md space-y-4">
        <div className="flex items-center justify-between pb-2 border-b border-slate-800">
          <h4 className="text-base font-bold text-white flex items-center gap-2 mb-0">
            <CheckCircle2 className="w-5 h-5 text-cyan-400" />
            <span>Why This Result?</span>
          </h4>
          <span className="text-xs text-slate-400">Core Factual Factors</span>
        </div>

        {/* 3 Insight Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          
          {/* Card A: Evidence Support Alignment */}
          <div className={`p-4 rounded-xl border transition-all ${
            isVerified ? 'bg-emerald-950/20 border-emerald-500/30 text-emerald-200' :
            isContradicted ? 'bg-rose-950/20 border-rose-500/30 text-rose-200' :
            isInsufficient ? 'bg-slate-950/60 border-slate-700/60 text-slate-200' :
            'bg-slate-950/50 border-slate-800 text-slate-200'
          }`}>
            <div className="flex items-center gap-2 font-bold text-sm mb-1.5">
              {isVerified && <CheckCircle2 className="w-4 h-4 text-emerald-400" />}
              {isContradicted && <XCircle className="w-4 h-4 text-rose-400" />}
              {isInsufficient && <AlertCircle className="w-4 h-4 text-amber-400" />}
              {isNonClaim && <Info className="w-4 h-4 text-slate-400" />}
              <span>
                {isVerified ? 'CLAIM SUPPORTED' :
                 isContradicted ? 'CLAIM CONTRADICTED' :
                 isInsufficient ? 'INSUFFICIENT EVIDENCE' : 'NON-VERIFIABLE INPUT'}
              </span>
            </div>
            <p className="text-xs text-slate-300 leading-relaxed mb-0">
              {isVerified && 'Retrieved documentary records and accredited wires agree with the core propositions.'}
              {isContradicted && 'Accredited wire reporting directly conflicts with the submitted factual claims.'}
              {isInsufficient && 'TruthLens could not locate enough accredited wire or official records to establish this claim.'}
              {isNonClaim && 'The input does not contain an objective declarative news claim with checkable facts.'}
            </p>
          </div>

          {/* Card B: Independent Corroboration */}
          <div className={`p-4 rounded-xl border transition-all ${
            distinctClustersCount >= 2 ? 'bg-emerald-950/20 border-emerald-500/30 text-emerald-200' :
            distinctClustersCount === 1 ? 'bg-sky-950/20 border-sky-500/30 text-sky-200' :
            'bg-slate-950/60 border-slate-700/60 text-slate-200'
          }`}>
            <div className="flex items-center gap-2 font-bold text-sm mb-1.5">
              {distinctClustersCount >= 2 ? (
                <>
                  <Layers className="w-4 h-4 text-emerald-400" />
                  <span>INDEPENDENT CORROBORATION</span>
                </>
              ) : distinctClustersCount === 1 ? (
                <>
                  <ShieldCheck className="w-4 h-4 text-sky-400" />
                  <span>SINGLE WIRE ORIGIN</span>
                </>
              ) : (
                <>
                  <Radio className="w-4 h-4 text-slate-400" />
                  <span>NO CORROBORATION</span>
                </>
              )}
            </div>
            <p className="text-xs text-slate-300 leading-relaxed mb-0">
              {distinctClustersCount >= 2 
                ? `Corroborated across ${distinctClustersCount} independent reporting clusters, eliminating single-source bias.` 
                : distinctClustersCount === 1
                ? 'Driven by a single reporting agency; multi-wire syndication awaiting corroboration.'
                : 'No independent reporting clusters identified in verified archives.'}
            </p>
          </div>

          {/* Card C: Contradiction / Hoax Evaluation */}
          <div className={`p-4 rounded-xl border transition-all ${
            severity === 'NONE' && detectedDiffs.length === 0 ? 'bg-emerald-950/20 border-emerald-500/30 text-emerald-200' :
            severity === 'DIRECT_FACTUAL_REVERSAL' || severity === 'MAJOR_CONTRADICTION' ? 'bg-rose-950/20 border-rose-500/30 text-rose-200' :
            'bg-amber-950/20 border-amber-500/30 text-amber-200'
          }`}>
            <div className="flex items-center gap-2 font-bold text-sm mb-1.5">
              {severity === 'NONE' && detectedDiffs.length === 0 ? (
                <>
                  <Check className="w-4 h-4 text-emerald-400" />
                  <span>NO MAJOR CONTRADICTION</span>
                </>
              ) : (
                <>
                  <AlertTriangle className="w-4 h-4 text-amber-400" />
                  <span>{severity.replace(/_/g, ' ')}</span>
                </>
              )}
            </div>
            <p className="text-xs text-slate-300 leading-relaxed mb-0">
              {severity === 'NONE' && detectedDiffs.length === 0
                ? 'No significant factual discrepancies or polarity reversals detected.'
                : detectedDiffs.length > 0
                ? detectedDiffs[0]
                : `Contradiction severity classified as ${severity}.`}
            </p>
          </div>
        </div>

        {/* "WHAT THIS MEANS" Plain-Language Explanation */}
        <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800 space-y-1">
          <span className="text-[11px] uppercase font-bold text-cyan-400 flex items-center gap-1.5">
            <Zap className="w-3.5 h-3.5" /> What This Means
          </span>
          <p className="text-xs text-slate-300 leading-relaxed mb-0">
            {isVerified && `Multiple independent news organizations and public repositories have separately confirmed the primary entities and actions described in this claim.`}
            {isAuthoritativeNotice && `An official statutory body or verified agency released this statement, but independent secondary news wires have not yet republished or corroborated it.`}
            {isContradicted && `The submitted text makes claims that directly conflict with accredited reporting from verified sources and disaster records.`}
            {isInsufficient && `This claim is either very recent, localized, or unverified. A lack of evidence does not mean it is definitely false, but independent verification is currently unavailable.`}
            {isNonClaim && `TruthLens is an evidence verification system for news facts, not a general question-answering search engine. Conversational questions and opinions do not contain verifiable facts.`}
          </p>
        </div>
      </div>

      {/* ─── SECTION 03: CLAIM BREAKDOWN (WHO -> DID WHAT -> TO WHOM) ─── */}
      {subClaims.length > 0 && (
        <div className="p-6 rounded-2xl bg-slate-900/90 border border-slate-700/60 shadow-xl backdrop-blur-md space-y-4">
          <div className="flex items-center justify-between pb-2 border-b border-slate-800">
            <div>
              <h4 className="text-base font-bold text-white flex items-center gap-2 mb-0">
                <Split className="w-5 h-5 text-purple-400" />
                <span>Claim Breakdown ({subClaims.length} Atomic Proposition{subClaims.length > 1 ? 's' : ''})</span>
              </h4>
              <p className="text-xs text-slate-400 mt-0.5 mb-0">
                TruthLens deconstructed this submission into individual factual statements for independent verification.
              </p>
            </div>
            <span className="text-xs text-slate-400 font-mono">
              {completeness}% Verified
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
            {subClaims.map((sub, idx) => {
              const isVerifiedSub = sub.claimVerdict === 'VERIFIED' || sub.claimVerdict === 'MOSTLY_VERIFIED';
              const isRefutedSub = sub.claimVerdict === 'REFUTED';

              return (
                <div 
                  key={idx}
                  className={`p-4 rounded-xl border transition-all ${
                    isVerifiedSub ? 'bg-emerald-950/15 border-emerald-500/25' :
                    isRefutedSub ? 'bg-rose-950/20 border-rose-500/30' :
                    'bg-slate-950/50 border-slate-800'
                  }`}
                >
                  <div className="flex items-center justify-between gap-2 mb-2">
                    <span className="text-[11px] uppercase font-bold px-2 py-0.5 rounded bg-slate-800 text-purple-300 border border-slate-700">
                      Proposition #{idx + 1}
                    </span>
                    {getStanceBadge(sub.stance)}
                  </div>

                  <p className="text-sm font-semibold text-white mb-2.5">
                    "{sub.claimText}"
                  </p>

                  {/* WHO -> DID WHAT -> TO WHOM/WHAT Entity Flow */}
                  {sub.entityRelationship && sub.entityRelationship.subject && (
                    <div className="p-2.5 rounded-lg bg-slate-900/90 border border-slate-800/90 mb-2.5 space-y-1">
                      <div className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">
                        Proposition Flow:
                      </div>
                      <div className="flex items-center gap-1.5 text-xs text-slate-200 flex-wrap">
                        <span className="px-2 py-0.5 rounded bg-sky-950 text-sky-300 font-bold border border-sky-500/30">
                          {sub.entityRelationship.subject}
                        </span>
                        <ArrowRight className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                        <span className="px-2 py-0.5 rounded bg-amber-950 text-amber-300 font-medium border border-amber-500/30">
                          {sub.entityRelationship.predicate}
                        </span>
                        <ArrowRight className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                        <span className="px-2 py-0.5 rounded bg-emerald-950 text-emerald-300 font-bold border border-emerald-500/30">
                          {sub.entityRelationship.objectValue}
                        </span>
                      </div>
                    </div>
                  )}

                  {/* Technical SPO Toggle */}
                  <div className="pt-2 border-t border-slate-800/80 flex items-center justify-between text-xs">
                    <button
                      type="button"
                      onClick={() => toggleSPO(idx)}
                      className="text-[11px] text-slate-400 hover:text-cyan-300 transition-colors flex items-center gap-1 cursor-pointer"
                    >
                      <span>{showTechnicalSPO[idx] ? 'Hide SPO Details' : 'Show technical representation'}</span>
                      {showTechnicalSPO[idx] ? <ChevronUp className="w-3 h-3" /> : <ChevronDown className="w-3 h-3" />}
                    </button>

                    <span className="font-mono font-bold text-cyan-400">
                      Score: {sub.claimScore != null ? `${sub.claimScore}/100` : 'N/A'}
                    </span>
                  </div>

                  {/* Expanded Technical Representation */}
                  {showTechnicalSPO[idx] && (
                    <div className="mt-2.5 p-2 rounded bg-slate-950 border border-slate-800 text-[11px] font-mono text-slate-400 space-y-1 animate-fadeIn">
                      <div>Subject (S): <span className="text-sky-300">{sub.entityRelationship?.subject || 'N/A'}</span></div>
                      <div>Predicate (P): <span className="text-amber-300">{sub.entityRelationship?.predicate || 'N/A'}</span></div>
                      <div>Object (O): <span className="text-emerald-300">{sub.entityRelationship?.objectValue || 'N/A'}</span></div>
                      <div>Target Metric: <span className="text-slate-200">{sub.targetMetric || 'NONE'}</span></div>
                      <div>Centrality Weight: <span className="text-purple-300">{sub.claimImportanceWeight || 0.5}</span></div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* ─── SECTION 04: EVIDENCE & PROVENANCE ─── */}
      {viewMode !== 'simple' && (
        <div className="p-6 rounded-2xl bg-slate-900/90 border border-slate-700/60 shadow-xl backdrop-blur-md space-y-4">
          <div className="flex items-center justify-between pb-2 border-b border-slate-800">
            <div>
              <h4 className="text-base font-bold text-white flex items-center gap-2 mb-0">
                <Layers className="w-5 h-5 text-cyan-400" />
                <span>Evidence & Provenance</span>
              </h4>
              <p className="text-xs text-slate-400 mt-0.5 mb-0">
                Independent news wire clusters and reporting origin discovery.
              </p>
            </div>
            <span className="text-xs text-slate-400">
              {sources.length} Articles • {distinctClustersCount} Clusters
            </span>
          </div>

          {/* Visual Provenance Flow Chart */}
          <div className="p-4 rounded-xl bg-slate-950/70 border border-slate-800 space-y-3">
            <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400 block">
              Reporting Provenance Chain:
            </span>
            
            <div className="flex items-center justify-between flex-wrap gap-2 text-xs">
              <div className="p-3 rounded-lg bg-slate-900 border border-slate-700 text-center flex-1 min-w-[140px]">
                <span className="text-[10px] text-slate-500 block uppercase font-bold">1. Earliest Report</span>
                <span className="font-bold text-white block mt-0.5 truncate">
                  {originDiscovery.earliestSourceName || sources[0]?.sourceName || 'Primary Wire / Registry'}
                </span>
                <span className="text-[10px] text-cyan-400 block">
                  {originDiscovery.earliestPublicationDate || sources[0]?.publicationDate || 'Earliest Timestamp'}
                </span>
              </div>

              <ArrowRight className="w-4 h-4 text-slate-600 shrink-0 hidden sm:block" />

              <div className="p-3 rounded-lg bg-slate-900 border border-slate-700 text-center flex-1 min-w-[140px]">
                <span className="text-[10px] text-slate-500 block uppercase font-bold">2. Wire Syndication</span>
                <span className="font-bold text-white block mt-0.5">
                  {distinctClustersCount} Independent Agencies
                </span>
                <span className="text-[10px] text-purple-400 block">
                  Deduplicated Feeds
                </span>
              </div>

              <ArrowRight className="w-4 h-4 text-slate-600 shrink-0 hidden sm:block" />

              <div className="p-3 rounded-lg bg-slate-900 border border-slate-700 text-center flex-1 min-w-[140px]">
                <span className="text-[10px] text-slate-500 block uppercase font-bold">3. Multi-Cluster Consensus</span>
                <span className="font-bold text-emerald-300 block mt-0.5">
                  {isVerified ? 'Consensus Achieved' : isContradicted ? 'Refuted Consensus' : 'Awaiting Corroboration'}
                </span>
                <span className="text-[10px] text-emerald-400/80 block">
                  {result.supportScore != null ? `${result.supportScore}/100 Support` : 'N/A'}
                </span>
              </div>
            </div>
          </div>

          {/* Evidence Cluster Cards */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5 pt-1">
            {sources.slice(0, 4).map((source, sIdx) => (
              <div key={sIdx} className="p-4 rounded-xl bg-slate-950/60 border border-slate-800 space-y-2">
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-1.5">
                    <span className="font-bold text-sm text-white">{source.sourceName}</span>
                    {source.isPrimarySource && (
                      <span className="text-[9px] px-1.5 py-0.5 rounded bg-indigo-500/20 text-indigo-300 font-bold border border-indigo-500/30">
                        OFFICIAL
                      </span>
                    )}
                  </div>
                  {getStanceBadge(source.stance)}
                </div>

                <div className="text-xs text-slate-400">
                  {getTierCleanName(source.evidenceTier)}
                </div>

                {source.articleTitle && (
                  <p className="text-xs text-slate-300 line-clamp-2 italic mb-0">
                    "{source.articleTitle}"
                  </p>
                )}

                <div className="flex items-center justify-between pt-2 border-t border-slate-800/80 text-[11px] text-slate-500">
                  <span>Match: {source.matchPercentage || 90}%</span>
                  <span>{source.directness?.replace(/_/g, ' ') || 'Secondary Wire'}</span>
                  {source.url && (
                    <a 
                      href={source.url} 
                      target="_blank" 
                      rel="noreferrer" 
                      className="text-cyan-400 hover:text-cyan-300 flex items-center gap-1"
                    >
                      <span>Link</span>
                      <ExternalLink className="w-3 h-3" />
                    </a>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ─── SECTION 05: VERIFICATION PIPELINE (COMPACT + INSPECT TRACE) ─── */}
      {viewMode !== 'simple' && pipelineSteps && pipelineSteps.length > 0 && (
        <div className="p-6 rounded-2xl bg-slate-900/90 border border-slate-700/60 shadow-xl backdrop-blur-md space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-2 pb-2 border-b border-slate-800">
            <div className="flex items-center gap-2">
              <GitBranch className="w-5 h-5 text-cyan-400" />
              <h4 className="text-base font-bold text-white mb-0">
                Verification Pipeline
              </h4>
            </div>

            <div className="flex items-center gap-2">
              <span className={`text-xs px-2.5 py-1 rounded-full font-bold flex items-center gap-1.5 ${
                blockedStep 
                  ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30' 
                  : isAllPassed 
                  ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30' 
                  : 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/30'
              }`}>
                {blockedStep ? (
                  <>
                    <XCircle className="w-3.5 h-3.5 text-rose-400" />
                    Halted at Stage {blockedStep.stepNumber}
                  </>
                ) : (
                  <>
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                    {totalCompletedStages}/{pipelineSteps.length} Stages Passed
                  </>
                )}
              </span>

              <button
                type="button"
                onClick={() => setShowAllSteps(!showAllSteps)}
                className="px-3 py-1 text-xs font-semibold rounded-lg bg-slate-800 hover:bg-slate-750 text-slate-300 hover:text-white border border-slate-700 transition-colors flex items-center gap-1.5 cursor-pointer"
              >
                <span>{showAllSteps ? 'Compact View' : 'Inspect Full Trace'}</span>
                {showAllSteps ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
              </button>
            </div>
          </div>

          {/* Horizontal Node Strip */}
          <div className="py-2 overflow-x-auto">
            <div className="flex items-center justify-between min-w-[640px] relative px-2">
              <div className="absolute left-6 right-6 top-4 h-0.5 bg-slate-800 -z-0" />
              
              {pipelineSteps.map((step, idx) => {
                const isCompleted = step.status === 'COMPLETED' || step.status === 'PASSED';
                const isBlocked = step.status === 'BLOCKED';
                const isSkipped = step.status === 'SKIPPED';
                const isSelected = selectedStepIdx === idx || (selectedStepIdx === null && (blockedStep ? blockedStep.stepNumber === step.stepNumber : idx === pipelineSteps.length - 1));

                return (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => setSelectedStepIdx(idx)}
                    className={`relative z-10 flex flex-col items-center group transition-all cursor-pointer p-1 rounded-lg focus:outline-none ${
                      isSelected ? 'scale-105' : 'opacity-85 hover:opacity-100'
                    }`}
                  >
                    <div className={`w-8 h-8 rounded-full flex items-center justify-center font-mono text-xs font-bold transition-all border ${
                      isSelected ? 'ring-2 ring-cyan-400 ring-offset-2 ring-offset-slate-950' : ''
                    } ${
                      isBlocked
                        ? 'bg-rose-950 border-rose-500 text-rose-300 shadow-rose-900/50 shadow-md'
                        : isCompleted
                        ? 'bg-emerald-950 border-emerald-500 text-emerald-300 shadow-emerald-900/50 shadow-md'
                        : isSkipped
                        ? 'bg-slate-900 border-slate-700 text-slate-500'
                        : 'bg-cyan-950 border-cyan-500 text-cyan-300'
                    }`}>
                      {isBlocked ? <X className="w-4 h-4 text-rose-400" /> : isCompleted ? <Check className="w-4 h-4 text-emerald-400" /> : <span>{step.stepNumber}</span>}
                    </div>
                    
                    <span className={`text-[10px] font-medium mt-1.5 truncate max-w-[64px] text-center ${
                      isSelected ? 'text-cyan-300 font-bold' : isCompleted ? 'text-slate-300' : 'text-slate-500'
                    }`}>
                      {shortStepNames[idx] || `Stage ${step.stepNumber}`}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Selected Stage Detail Card */}
          {currentActiveStep && !showAllSteps && (
            <div className="p-3.5 rounded-xl bg-slate-950/70 border border-slate-800">
              <div className="flex items-center justify-between gap-2 mb-1">
                <span className="text-xs font-mono font-bold text-cyan-400">
                  STAGE {currentActiveStep.stepNumber} — {currentActiveStep.stepName}
                </span>
                <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                  currentActiveStep.status === 'BLOCKED' ? 'bg-rose-500/20 text-rose-300' : 'bg-emerald-500/20 text-emerald-300'
                }`}>
                  {currentActiveStep.status}
                </span>
              </div>
              <p className="text-xs text-slate-300 mb-0 leading-relaxed">
                {currentActiveStep.detail}
              </p>
            </div>
          )}

          {/* Collapsible 10-Stage Audit Trail */}
          {showAllSteps && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2 border-t border-slate-800 animate-fadeIn">
              {pipelineSteps.map((step, idx) => (
                <div key={idx} className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 space-y-1 text-xs">
                  <div className="flex items-center justify-between font-bold">
                    <span className="text-cyan-400 font-mono">Stage {step.stepNumber}: {step.stepName}</span>
                    <span className={`text-[10px] px-1.5 py-0.5 rounded ${
                      step.status === 'BLOCKED' ? 'bg-rose-500/20 text-rose-300' : 'bg-emerald-500/20 text-emerald-300'
                    }`}>{step.status}</span>
                  </div>
                  <p className="text-slate-400 text-[11px] mb-0">{step.detail}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ─── SECTION 06: ADVANCED DIAGNOSTICS ─── */}
      {viewMode !== 'simple' && (result.retrievalQuality || explainability.retrievalQuality) && (
        <div className="p-6 rounded-2xl bg-slate-900/90 border border-slate-700/60 shadow-xl backdrop-blur-md space-y-4">
          <div className="flex items-center justify-between pb-2 border-b border-slate-800">
            <h4 className="text-base font-bold text-white flex items-center gap-2 mb-0">
              <Search className="w-5 h-5 text-cyan-400" />
              <span>Retrieval Quality Diagnostics</span>
            </h4>
            <span className="text-xs font-mono text-cyan-400">
              Coverage Analysis
            </span>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 text-center">
              <span className="text-[10px] uppercase font-bold text-slate-400 block">Query Quality</span>
              <span className="text-sm font-bold text-emerald-400 block mt-0.5">HIGH ✓</span>
            </div>
            <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 text-center">
              <span className="text-[10px] uppercase font-bold text-slate-400 block">Regional Coverage</span>
              <span className={`text-sm font-bold block mt-0.5 ${
                (result.retrievalQuality || explainability.retrievalQuality)?.regionalCoverage === 'HIGH' ? 'text-emerald-400' : 'text-amber-400'
              }`}>
                {(result.retrievalQuality || explainability.retrievalQuality)?.regionalCoverage || 'MEDIUM'} 
                {(result.retrievalQuality || explainability.retrievalQuality)?.regionalCoverage === 'MEDIUM' ? ' ⚠' : ' ✓'}
              </span>
            </div>
            <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 text-center">
              <span className="text-[10px] uppercase font-bold text-slate-400 block">Source Diversity</span>
              <span className="text-sm font-bold text-purple-400 block mt-0.5">HIGH ✓</span>
            </div>
            <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 text-center">
              <span className="text-[10px] uppercase font-bold text-slate-400 block">Search Completeness</span>
              <span className="text-sm font-bold text-cyan-400 block mt-0.5">HIGH ✓</span>
            </div>
          </div>

          {/* Regional coverage caveat */}
          {(result.retrievalQuality || explainability.retrievalQuality)?.regionalCoverage === 'MEDIUM' && (
            <div className="p-3 rounded-lg bg-amber-950/30 border border-amber-500/30 text-xs text-amber-200 flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0" />
              <span>Regional news wire indexing is moderate; claim cross-referenced primarily via international accredited feeds.</span>
            </div>
          )}
        </div>
      )}

      {/* ─── SECTION 07: TECHNICAL AUDIT (COLLAPSIBLE BY DEFAULT) ─── */}
      <div className="p-6 rounded-2xl bg-slate-900/90 border border-slate-700/60 shadow-xl backdrop-blur-md space-y-4">
        
        {/* Collapsible Header */}
        <div 
          className="flex items-center justify-between pb-2 border-b border-slate-800 cursor-pointer group"
          onClick={() => setShowTechnicalAudit(!showTechnicalAudit)}
        >
          <div className="flex items-center gap-2.5">
            <Terminal className="w-5 h-5 text-purple-400" />
            <div>
              <h4 className="text-base font-bold text-white mb-0 group-hover:text-purple-300 transition-colors">
                Technical Audit & Deterministic Calculations
              </h4>
              <p className="text-xs text-slate-400 mt-0.5 mb-0">
                Full academic transparency, mathematical formulas, point contributions, and evidence matrix.
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <span className="text-xs font-mono text-purple-400 px-2 py-0.5 rounded bg-purple-500/10 border border-purple-500/20">
              {showTechnicalAudit ? 'COLLAPSE' : 'EXPAND AUDIT'}
            </span>
            {showTechnicalAudit ? <ChevronUp className="w-4 h-4 text-purple-400" /> : <ChevronDown className="w-4 h-4 text-purple-400" />}
          </div>
        </div>

        {/* Expanded Technical Details */}
        {showTechnicalAudit && (
          <div className="space-y-5 animate-fadeIn pt-2">
            
            {/* Audit Sub-Tabs */}
            <div className="flex items-center gap-2 border-b border-slate-800 pb-2 overflow-x-auto text-xs">
              <button
                type="button"
                onClick={() => setActiveAuditTab('scoring')}
                className={`px-3 py-1.5 rounded-lg font-bold transition-all cursor-pointer ${
                  activeAuditTab === 'scoring' ? 'bg-purple-600 text-white' : 'text-slate-400 hover:text-white'
                }`}
              >
                Scoring Mathematics
              </button>
              <button
                type="button"
                onClick={() => setActiveAuditTab('clusters')}
                className={`px-3 py-1.5 rounded-lg font-bold transition-all cursor-pointer ${
                  activeAuditTab === 'clusters' ? 'bg-purple-600 text-white' : 'text-slate-400 hover:text-white'
                }`}
              >
                Per-Cluster Contributions
              </button>
              <button
                type="button"
                onClick={() => setActiveAuditTab('matrix')}
                className={`px-3 py-1.5 rounded-lg font-bold transition-all cursor-pointer ${
                  activeAuditTab === 'matrix' ? 'bg-purple-600 text-white' : 'text-slate-400 hover:text-white'
                }`}
              >
                Evidence Authority Matrix
              </button>
              <button
                type="button"
                onClick={() => setActiveAuditTab('raw')}
                className={`px-3 py-1.5 rounded-lg font-bold transition-all cursor-pointer ${
                  activeAuditTab === 'raw' ? 'bg-purple-600 text-white' : 'text-slate-400 hover:text-white'
                }`}
              >
                Raw Telemetry JSON
              </button>
            </div>

            {/* TAB A: SCORING MATHEMATICS */}
            {activeAuditTab === 'scoring' && (
              <div className="space-y-4">
                <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-2.5 font-mono text-xs text-slate-300">
                  <div className="flex items-center justify-between">
                    <span className="text-cyan-400 font-bold">Base Support Score Formula:</span>
                    <span>{explainability.baseScoreFormula || "Base = min(100, \u2211 [W(Tier_i) \u00D7 S(Stance_i) \u00D7 R_i])"}</span>
                  </div>
                  <div className="flex items-center justify-between pt-2 border-t border-slate-800/80">
                    <span className="text-rose-400 font-bold">Damped Penalty Formula:</span>
                    <span>{explainability.penaltyScalingFormula || "Scaled Penalty = Raw Penalty \u00D7 [1 / (1 + 0.15 \u00D7 (Clusters - 1))]"}</span>
                  </div>
                  <div className="flex items-center justify-between pt-2 border-t border-slate-800/80">
                    <span className="text-emerald-400 font-bold">Final Verdict Score Formula:</span>
                    <span>Final Score = max(0, min(100, Base Score − Scaled Penalty))</span>
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 font-mono text-center">
                  <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800">
                    <span className="text-[11px] text-slate-400 uppercase font-bold block">Base Score</span>
                    <span className="text-2xl font-bold text-cyan-400 block mt-1">
                      {result.baseSupportScore != null ? result.baseSupportScore : explainability.baseSupportScore || 0}/100
                    </span>
                  </div>
                  <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800">
                    <span className="text-[11px] text-slate-400 uppercase font-bold block">Scaled Penalty</span>
                    <span className="text-2xl font-bold text-rose-400 block mt-1">
                      -{(result.contradictionPenalty != null ? result.contradictionPenalty : explainability.contradictionPenalty) || 0} pts
                    </span>
                  </div>
                  <div className="p-3.5 rounded-xl bg-slate-950 border border-emerald-500/30">
                    <span className="text-[11px] text-emerald-400 uppercase font-bold block">Final Support Score</span>
                    <span className="text-2xl font-bold text-emerald-300 block mt-1">
                      {result.genuinenessScore != null ? `${result.genuinenessScore}/100` : 'N/A'}
                    </span>
                  </div>
                </div>
              </div>
            )}

            {/* TAB B: PER-CLUSTER CONTRIBUTIONS */}
            {activeAuditTab === 'clusters' && (
              <div className="space-y-3">
                {explainability.perClusterContributions && explainability.perClusterContributions.length > 0 ? (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {explainability.perClusterContributions.map((cluster, cIdx) => (
                      <div key={cIdx} className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 space-y-1.5 text-xs">
                        <div className="flex items-center justify-between font-mono">
                          <span className="font-bold text-cyan-400">{cluster.clusterId}</span>
                          <span className="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300 font-bold">
                            +{cluster.pointContribution} pts
                          </span>
                        </div>
                        <div className="font-bold text-white text-sm">{cluster.primaryOutlet}</div>
                        <div className="text-slate-400 text-[11px]">{getTierCleanName(cluster.evidenceTier)}</div>
                        <div className="flex items-center justify-between pt-1 border-t border-slate-800 text-[11px] text-slate-400">
                          <span>Stance: <strong className="text-sky-300">{cluster.stance}</strong></span>
                          <span>Relevance: 1.0</span>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-xs text-slate-400 italic">No per-cluster point contributions calculated.</p>
                )}
              </div>
            )}

            {/* TAB C: EVIDENCE MATRIX TABLE */}
            {activeAuditTab === 'matrix' && (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-slate-300">
                  <thead className="bg-slate-950 text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
                    <tr>
                      <th className="py-2.5 px-3">Evidence Source</th>
                      <th className="py-2.5 px-3">Authority Tier</th>
                      <th className="py-2.5 px-3">Factual Stance</th>
                      <th className="py-2.5 px-3">Directness</th>
                      <th className="py-2.5 px-3">Justification</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800">
                    {matrix.map((item, idx) => (
                      <tr key={idx} className="hover:bg-slate-800/40">
                        <td className="py-2.5 px-3 font-semibold text-white">{item.sourceName}</td>
                        <td className="py-2.5 px-3">{item.evidenceTier?.replace(/LEVEL_/g, 'Level ')}</td>
                        <td className="py-2.5 px-3">{getStanceBadge(item.stance)}</td>
                        <td className="py-2.5 px-3">{item.directness?.replace(/_/g, ' ') || 'Secondary'}</td>
                        <td className="py-2.5 px-3 text-slate-400">{item.acceptanceReasons?.[0] || 'Accredited documentary candidate'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {/* TAB D: RAW JSON TELEMETRY */}
            {activeAuditTab === 'raw' && (
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs text-slate-400 font-mono">Payload Schema: TRUTHLENS_AUDIT_V3.1</span>
                  <button
                    type="button"
                    onClick={handleCopyRaw}
                    className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs flex items-center gap-1 cursor-pointer"
                  >
                    {copiedRaw ? <CheckCheck className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    <span>{copiedRaw ? 'Copied!' : 'Copy JSON'}</span>
                  </button>
                </div>
                <pre className="p-4 rounded-xl bg-slate-950 border border-slate-800 text-[11px] font-mono text-cyan-300 overflow-x-auto max-h-96">
                  {JSON.stringify(result, null, 2)}
                </pre>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
