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
  MinusCircle
} from 'lucide-react';

export default function ExplainabilityCard({ result }) {
  if (!result) return null;

  const [selectedStepIdx, setSelectedStepIdx] = useState(null);
  const [showAllSteps, setShowAllSteps] = useState(false);

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

  const getConfidenceBadge = (conf, score) => {
    switch (conf?.toUpperCase()) {
      case 'HIGH':
        return (
          <span className="px-3 py-1 text-xs font-semibold rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 flex items-center gap-1.5 shadow-sm">
            <ShieldCheck className="w-3.5 h-3.5" /> High Confidence ({score}%)
          </span>
        );
      case 'LOW':
        return (
          <span className="px-3 py-1 text-xs font-semibold rounded-full bg-rose-500/20 text-rose-300 border border-rose-500/30 flex items-center gap-1.5 shadow-sm">
            <AlertTriangle className="w-3.5 h-3.5" /> Low Confidence ({score}%)
          </span>
        );
      default:
        return (
          <span className="px-3 py-1 text-xs font-semibold rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30 flex items-center gap-1.5 shadow-sm">
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

  const getCentralityBadge = (centrality) => {
    switch (centrality) {
      case 'PRIMARY_CLAIM':
        return <span className="px-2 py-0.5 text-[10px] font-bold uppercase rounded bg-purple-500/20 text-purple-300 border border-purple-500/30">Primary Core Fact</span>;
      case 'SUPPORTING_CLAIM':
        return <span className="px-2 py-0.5 text-[10px] font-medium uppercase rounded bg-sky-500/20 text-sky-300 border border-sky-500/30">Supporting Detail</span>;
      default:
        return <span className="px-2 py-0.5 text-[10px] font-medium uppercase rounded bg-slate-500/20 text-slate-400 border border-slate-600/30">Contextual Fact</span>;
    }
  };

  const getDistortionBadge = (type) => {
    if (!type || type === 'NONE') return null;
    switch (type) {
      case 'LOCATION_DISTORTION':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-rose-500/20 text-rose-300 border border-rose-500/30 flex items-center gap-1"><MapPin className="w-3.5 h-3.5" /> Location Discrepancy</span>;
      case 'ATTRIBUTION_DISTORTION':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-purple-500/20 text-purple-300 border border-purple-500/30 flex items-center gap-1"><Building2 className="w-3.5 h-3.5" /> Attribution Mismatch</span>;
      case 'NUMERICAL_DISTORTION':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30 flex items-center gap-1"><AlertTriangle className="w-3.5 h-3.5" /> Numerical Disparity</span>;
      case 'POLARITY_DISTORTION':
        return <span className="px-2.5 py-1 text-xs font-bold rounded-full bg-rose-500/25 text-rose-300 border border-rose-500/40 flex items-center gap-1"><AlertCircle className="w-3.5 h-3.5" /> Direct Polarity Reversal</span>;
      case 'CONTEXT_DISTORTION':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30 flex items-center gap-1"><Info className="w-3.5 h-3.5" /> Context Distortion</span>;
      default:
        return null;
    }
  };

  const getAsOfBadge = (status) => {
    switch (status) {
      case 'SUPPORTED_AT_CLAIM_TIME':
        return <span className="px-2.5 py-1 text-xs font-medium rounded-full bg-sky-500/20 text-sky-300 border border-sky-500/30 flex items-center gap-1"><Clock className="w-3.5 h-3.5" /> As-Of Claim Time: Supported (Developing)</span>;
      case 'OUTDATED_SUPERSEDED':
        return <span className="px-2.5 py-1 text-xs font-medium rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30 flex items-center gap-1"><Clock className="w-3.5 h-3.5" /> Status: Outdated / Superseded</span>;
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
      return <span className="px-2 py-0.5 text-[11px] font-semibold rounded bg-cyan-500/20 text-cyan-300 border border-cyan-500/30">Level 4 Reference Archive</span>;
    }
    if (t.includes('LEVEL_5')) {
      return <span className="px-2 py-0.5 text-[11px] font-semibold rounded bg-rose-500/20 text-rose-300 border border-rose-500/30">Level 5 User Social</span>;
    }
    return <span className="px-2 py-0.5 text-[11px] font-semibold rounded bg-sky-500/20 text-sky-300 border border-sky-500/30">Level 2 News Wire</span>;
  };

  const shortStepNames = [
    'Ingestion',
    'Classify',
    'Extract',
    'Verifiability',
    'Retrieval',
    'Validation',
    'Stance',
    'Fusion',
    'Forensics',
    'XAI Report'
  ];

  const totalCompletedStages = pipelineSteps.filter(s => s.status === 'COMPLETED' || s.status === 'PASSED').length;
  const isAllPassed = pipelineSteps.length > 0 && totalCompletedStages === pipelineSteps.length;
  const blockedStep = pipelineSteps.find(s => s.status === 'BLOCKED');

  const currentActiveStep = selectedStepIdx !== null 
    ? pipelineSteps[selectedStepIdx] 
    : (blockedStep || pipelineSteps[pipelineSteps.length - 1] || null);

  return (
    <div className="space-y-6">
      {/* 1. Header & Executive Metrics Banner */}
      <div className="p-6 rounded-2xl bg-gradient-to-r from-slate-900/95 via-slate-850/90 to-slate-900/95 border border-slate-700/60 shadow-xl backdrop-blur-md">
        <div className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b border-slate-700/50">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs uppercase tracking-wider font-bold text-sky-400">TruthLens Claim-Contextual Decision Layer</span>
            </div>
            <h3 className="text-xl font-bold text-white mt-1 flex items-center gap-2.5">
              <Scale className="w-5 h-5 text-sky-400" />
              Evidence Synthesis & Explainability Report
            </h3>
            <p className="text-xs text-slate-400 mt-1 mb-0">
              Multi-source epistemic synthesis, pipeline trace audit, and contradiction scoring breakdown.
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            {getAsOfBadge(asOfStatus)}
            {getDistortionBadge(distortionType)}
            {getConfidenceBadge(confidence, confidenceScore)}
          </div>
        </div>

        {/* 2. Interactive Streamlined Pipeline Stepper (Replacing Congested 10-Box Wall) */}
        {pipelineSteps && pipelineSteps.length > 0 && (
          <div className="mt-5 p-4 sm:p-5 rounded-xl bg-slate-950/70 border border-slate-800/90 shadow-inner">
            <div className="flex flex-wrap items-center justify-between gap-2 pb-3 mb-3 border-b border-slate-800/80">
              <div className="flex items-center gap-2">
                <GitBranch className="w-4 h-4 text-cyan-400" />
                <span className="text-xs font-bold uppercase tracking-wider text-slate-200">
                  Verification Pipeline Stepper
                </span>
              </div>
              <div className="flex items-center gap-2">
                <span className={`text-xs px-2.5 py-0.5 rounded-full font-semibold flex items-center gap-1.5 ${
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
                  className="px-2.5 py-1 text-xs font-medium rounded-lg bg-slate-800 hover:bg-slate-750 text-slate-300 hover:text-white border border-slate-700 transition-colors flex items-center gap-1.5 cursor-pointer"
                >
                  <span>{showAllSteps ? 'Compact View' : 'Inspect Full Trace'}</span>
                  {showAllSteps ? <ChevronUp className="w-3.5 h-3.5 text-slate-400" /> : <ChevronDown className="w-3.5 h-3.5 text-slate-400" />}
                </button>
              </div>
            </div>

            {/* Visual Stepper Nodes Strip */}
            <div className="py-2 overflow-x-auto">
              <div className="flex items-center justify-between min-w-[620px] relative px-2">
                {/* Connecting background track */}
                <div className="absolute left-6 right-6 top-4 h-0.5 bg-slate-800 -z-0" />
                
                {pipelineSteps.map((step, idx) => {
                  const isCompleted = step.status === 'COMPLETED' || step.status === 'PASSED';
                  const isBlocked = step.status === 'BLOCKED';
                  const isSkipped = step.status === 'SKIPPED';
                  const isNotExecuted = step.status === 'NOT_EXECUTED';
                  const isSelected = selectedStepIdx === idx || (selectedStepIdx === null && (blockedStep ? blockedStep.stepNumber === step.stepNumber : idx === pipelineSteps.length - 1));

                  return (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => setSelectedStepIdx(idx)}
                      className={`relative z-10 flex flex-col items-center group transition-all cursor-pointer p-1 rounded-lg focus:outline-none ${
                        isSelected ? 'scale-105' : 'opacity-85 hover:opacity-100'
                      }`}
                      title={`${step.stepNumber}. ${step.stepName} (${step.status})`}
                    >
                      <div className={`w-8 h-8 rounded-full flex items-center justify-center font-mono text-xs font-bold transition-all border ${
                        isSelected ? 'ring-2 ring-cyan-400 ring-offset-2 ring-offset-slate-950' : ''
                      } ${
                        isBlocked
                          ? 'bg-rose-950 border-rose-500 text-rose-300 shadow-rose-900/50 shadow-md'
                          : isCompleted
                          ? 'bg-emerald-950 border-emerald-500 text-emerald-300 shadow-emerald-900/50 shadow-md'
                          : isSkipped || isNotExecuted
                          ? 'bg-slate-900 border-slate-700 text-slate-500'
                          : 'bg-cyan-950 border-cyan-500 text-cyan-300'
                      }`}>
                        {isBlocked ? (
                          <X className="w-4 h-4 text-rose-400" />
                        ) : isCompleted ? (
                          <Check className="w-4 h-4 text-emerald-400" />
                        ) : isSkipped ? (
                          <MinusCircle className="w-3.5 h-3.5 text-slate-400" />
                        ) : (
                          <span>{step.stepNumber}</span>
                        )}
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

            {/* Selected Stage Detail Box (Progressive Disclosure) */}
            {currentActiveStep && !showAllSteps && (
              <div className="mt-3.5 p-3.5 rounded-lg bg-slate-900/80 border border-slate-700/70 transition-all">
                <div className="flex flex-wrap items-center justify-between gap-2 mb-1.5">
                  <div className="flex items-center gap-2">
                    <span className="text-[11px] font-mono font-bold px-2 py-0.5 rounded bg-slate-800 text-cyan-400 border border-slate-700">
                      STAGE {currentActiveStep.stepNumber}
                    </span>
                    <h5 className="text-sm font-bold text-white mb-0">
                      {currentActiveStep.stepName}
                    </h5>
                  </div>
                  <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                    currentActiveStep.status === 'BLOCKED' ? 'bg-rose-500/20 text-rose-300 border border-rose-500/40' :
                    currentActiveStep.status === 'COMPLETED' || currentActiveStep.status === 'PASSED' ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40' :
                    'bg-slate-800 text-slate-400 border border-slate-700'
                  }`}>
                    {currentActiveStep.status}
                  </span>
                </div>
                <p className="text-xs text-slate-300 mb-0 font-sans leading-relaxed">
                  {currentActiveStep.detail}
                </p>
              </div>
            )}

            {/* Collapsible Full Step-by-Step Technical Trace Accordion */}
            {showAllSteps && (
              <div className="mt-4 pt-3 border-t border-slate-800 space-y-2.5 animate-fadeIn">
                <div className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2 flex items-center gap-1.5">
                  <Info className="w-3.5 h-3.5 text-cyan-400" />
                  Full 10-Stage Pipeline Audit Trail
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                  {pipelineSteps.map((step, idx) => {
                    const isCompleted = step.status === 'COMPLETED' || step.status === 'PASSED';
                    const isBlocked = step.status === 'BLOCKED';
                    return (
                      <div 
                        key={idx}
                        className={`p-3 rounded-lg border text-left transition-all ${
                          isBlocked 
                            ? 'bg-rose-950/30 border-rose-500/40 text-rose-200' 
                            : isCompleted 
                            ? 'bg-slate-900/60 border-slate-700/70 text-slate-200' 
                            : 'bg-slate-900/30 border-slate-800/80 text-slate-500'
                        }`}
                      >
                        <div className="flex items-center justify-between gap-1 mb-1">
                          <span className="text-[10px] font-mono font-bold text-cyan-400">
                            STAGE {step.stepNumber}
                          </span>
                          <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded ${
                            isBlocked ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30' :
                            isCompleted ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' :
                            'bg-slate-800 text-slate-500 border border-slate-700'
                          }`}>
                            {isBlocked ? '✗ BLOCKED' : isCompleted ? '✓ PASSED' : step.status}
                          </span>
                        </div>
                        <div className="text-xs font-bold text-white mb-0.5">{step.stepName}</div>
                        <p className="text-[11px] text-slate-400 leading-snug mb-0">
                          {step.detail}
                        </p>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
        )}

        {/* 3. Evidence Completeness & Context Info */}
        <div className="mt-4 grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-700/50 space-y-2">
            <div className="flex items-center justify-between text-xs font-bold uppercase tracking-wider">
              <span className="text-slate-300 flex items-center gap-1.5">
                <Activity className="w-4 h-4 text-sky-400" />
                Evidence Completeness
              </span>
              <span className="text-sky-400 font-mono font-bold text-sm">{completeness}% Corroborated</span>
            </div>
            <div className="w-full bg-slate-800 rounded-full h-2.5 overflow-hidden">
              <div 
                className="bg-gradient-to-r from-sky-500 to-emerald-400 h-2.5 rounded-full transition-all duration-500" 
                style={{ width: `${Math.max(5, completeness)}%` }}
              />
            </div>
            <p className="text-[11px] text-slate-400 leading-snug mb-0">
              {subClaims.length > 1 ? 
                `${subClaims.filter(s => s.claimVerdict === 'VERIFIED' || s.claimVerdict === 'MOSTLY_VERIFIED').length} of ${subClaims.length} atomic factual components independently substantiated.` :
                "Factual assertion evaluated against contextual authoritative records."}
            </p>
          </div>

          {context.domain && (
            <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-700/50 space-y-1.5">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
                <Globe className="w-4 h-4 text-emerald-400" />
                Claim Context & Target Authorities
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

        {/* 4. Base Support Score & Contradiction Penalty Breakdown */}
        {((result.baseSupportScore != null && result.contradictionPenalty != null) || (explainability.baseSupportScore != null && explainability.contradictionPenalty != null)) && (
          <div className="mt-4 p-4 rounded-xl bg-slate-950/70 border border-slate-700/60 shadow-lg space-y-3">
            <div className="flex items-center justify-between pb-2 border-b border-slate-800/80">
              <div className="flex items-center gap-2">
                <Scale className="w-4 h-4 text-emerald-400" />
                <span className="text-xs font-bold uppercase tracking-wider text-slate-200">
                  Normalized Evidence Scoring & Contradiction Penalty Model
                </span>
              </div>
              <span className="text-xs font-mono font-bold text-sky-400">
                Net Score = max(0, Base - Penalty)
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div className="p-3.5 rounded-lg bg-slate-900/80 border border-slate-800 text-center">
                <span className="text-[11px] uppercase font-bold text-slate-400 block mb-1">Base Support Score</span>
                <span className="text-2xl font-mono font-bold text-sky-400">
                  {result.baseSupportScore != null ? result.baseSupportScore : explainability.baseSupportScore}/100
                </span>
                <span className="text-[10px] text-slate-500 block mt-1">Multi-wire Corroboration Strength</span>
              </div>

              <div className="p-3.5 rounded-lg bg-slate-900/80 border border-slate-800 text-center">
                <span className="text-[11px] uppercase font-bold text-slate-400 block mb-1">Contradiction Penalty</span>
                <span className={`text-2xl font-mono font-bold ${(result.contradictionPenalty || explainability.contradictionPenalty || 0) > 0 ? 'text-rose-400' : 'text-slate-400'}`}>
                  -{(result.contradictionPenalty != null ? result.contradictionPenalty : explainability.contradictionPenalty) || 0} pts
                </span>
                <span className="text-[10px] text-slate-500 block mt-1">
                  Severity: {severity.replace(/_/g, ' ')}
                </span>
              </div>

              <div className="p-3.5 rounded-lg bg-emerald-950/20 border border-emerald-500/30 text-center">
                <span className="text-[11px] uppercase font-bold text-emerald-400 block mb-1">Final Support Score</span>
                <span className="text-2xl font-mono font-bold text-emerald-300">
                  {(result.supportScore != null ? result.supportScore : explainability.finalSupportScore) != null ? `${result.supportScore || explainability.finalSupportScore}/100` : 'N/A'}
                </span>
                <span className="text-[10px] text-emerald-400/80 block mt-1">Calibrated Epistemic Rating</span>
              </div>
            </div>
          </div>
        )}

        {/* 5. Structured Corroboration & Discrepancy Checklists */}
        <div className="mt-4 grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="p-4 rounded-xl bg-slate-950/50 border border-emerald-500/20 space-y-2.5">
            <h4 className="text-xs font-bold uppercase tracking-wider text-emerald-400 flex items-center gap-1.5">
              <CheckCircle2 className="w-4 h-4" /> Corroborated Findings
            </h4>
            {positiveChecklist.length > 0 ? (
              <ul className="space-y-2 text-sm text-slate-200 ps-0 mb-0 list-none">
                {positiveChecklist.map((item, idx) => (
                  <li key={idx} className="flex items-start gap-2">
                    <span className="text-emerald-400 font-bold mt-0.5 shrink-0">✓</span>
                    <span className="leading-snug">{item}</span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-xs text-slate-400 italic mb-0">No positive independent confirmations found.</p>
            )}
          </div>

          <div className="p-4 rounded-xl bg-slate-950/50 border border-amber-500/20 space-y-2.5">
            <h4 className="text-xs font-bold uppercase tracking-wider text-amber-400 flex items-center gap-1.5">
              <AlertTriangle className="w-4 h-4" /> Discrepancies & Cautionary Flags
            </h4>
            {warningChecklist.length > 0 ? (
              <ul className="space-y-2 text-sm text-slate-200 ps-0 mb-0 list-none">
                {warningChecklist.map((item, idx) => (
                  <li key={idx} className="flex items-start gap-2">
                    <span className="text-amber-400 font-bold mt-0.5 shrink-0">⚠</span>
                    <span className="leading-snug">{item}</span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-xs text-slate-400 italic mb-0">No significant factual or numerical discrepancies detected.</p>
            )}
          </div>
        </div>

        {/* 6. Detected Factual Differences / Distortions Callout */}
        {detectedDiffs.length > 0 && (
          <div className="mt-4 p-4 rounded-xl bg-rose-950/30 border border-rose-500/30 space-y-2">
            <h4 className="text-xs font-bold uppercase tracking-wider text-rose-400 flex items-center gap-1.5">
              <AlertCircle className="w-4 h-4" /> Detected Factual Distortions
            </h4>
            <div className="space-y-1.5 text-sm text-rose-200">
              {detectedDiffs.map((diff, idx) => (
                <div key={idx} className="flex items-start gap-2">
                  <span className="font-bold text-rose-400 shrink-0">→</span>
                  <span>{diff}</span>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* 7. Retrieval Quality Diagnostics Panel */}
      {(result.retrievalQuality || explainability.retrievalQuality) && (
        <div className="p-5 rounded-2xl bg-slate-900/80 border border-slate-700/60 shadow-xl backdrop-blur-md space-y-3">
          <div className="flex items-center justify-between pb-2 border-b border-slate-700/50">
            <h4 className="text-sm font-bold text-white flex items-center gap-2">
              <Search className="w-4 h-4 text-cyan-400" />
              Retrieval Quality & Coverage Diagnostics
            </h4>
            <span className="text-xs font-mono text-cyan-400">
              Query Quality: {(result.retrievalQuality || explainability.retrievalQuality).queryQuality || 'HIGH'}
            </span>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-1">
            <div className="p-3 rounded-lg bg-slate-950/60 border border-slate-800 text-center">
              <span className="text-[10px] uppercase font-bold text-slate-400 block mb-0.5">Regional Coverage</span>
              <span className={`text-sm font-bold block ${
                (result.retrievalQuality || explainability.retrievalQuality).regionalCoverage === 'HIGH' ? 'text-emerald-400' :
                (result.retrievalQuality || explainability.retrievalQuality).regionalCoverage === 'MEDIUM' ? 'text-sky-400' : 'text-slate-400'
              }`}>
                {(result.retrievalQuality || explainability.retrievalQuality).regionalCoverage || 'NONE'}
              </span>
            </div>

            <div className="p-3 rounded-lg bg-slate-950/60 border border-slate-800 text-center">
              <span className="text-[10px] uppercase font-bold text-slate-400 block mb-0.5">Source Diversity</span>
              <span className="text-sm font-bold text-purple-400 block">
                {(result.retrievalQuality || explainability.retrievalQuality).sourceDiversity || 'MEDIUM'}
              </span>
            </div>

            <div className="p-3 rounded-lg bg-slate-950/60 border border-slate-800 text-center">
              <span className="text-[10px] uppercase font-bold text-slate-400 block mb-0.5">Search Completeness</span>
              <span className="text-sm font-bold text-emerald-400 block">
                {(result.retrievalQuality || explainability.retrievalQuality).searchCompleteness || 'HIGH'}
              </span>
            </div>

            <div className="p-3 rounded-lg bg-slate-950/60 border border-slate-800 text-center">
              <span className="text-[10px] uppercase font-bold text-slate-400 block mb-0.5">Sources Searched</span>
              <span className="text-sm font-mono font-bold text-white block">
                {(result.retrievalQuality || explainability.retrievalQuality).sourcesSearchedCount || audit.sourcesRetrieved || 0}
              </span>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2 pt-1 text-xs text-slate-400">
            <span className="flex items-center gap-1">
              <span className={`w-2 h-2 rounded-full ${(result.retrievalQuality || explainability.retrievalQuality).officialSourcesSearched ? 'bg-emerald-400' : 'bg-slate-600'}`}></span>
              Official Authorities Searched
            </span>
            <span>•</span>
            <span className="flex items-center gap-1">
              <span className={`w-2 h-2 rounded-full ${(result.retrievalQuality || explainability.retrievalQuality).regionSpecificSourcesSearched ? 'bg-emerald-400' : 'bg-slate-600'}`}></span>
              Regional Sources Included
            </span>
            <span>•</span>
            <span className="flex items-center gap-1">
              <span className={`w-2 h-2 rounded-full ${(result.retrievalQuality || explainability.retrievalQuality).internationalSourcesSearched ? 'bg-emerald-400' : 'bg-slate-600'}`}></span>
              International Wires
            </span>
          </div>
        </div>
      )}

      {/* 8. Evidence Retrieval Audit Trail */}
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
            <p className="text-xs text-slate-400 italic pt-1 mb-0">{audit.auditSummary}</p>
          )}
        </div>
      )}

      {/* 9. Atomic Sub-Claims Decomposition with Entity-Relationship Triples */}
      {subClaims.length > 0 && (
        <div className="p-5 rounded-2xl bg-slate-900/80 border border-slate-700/60 shadow-xl backdrop-blur-md space-y-3">
          <div className="flex items-center justify-between pb-2 border-b border-slate-700/50">
            <h4 className="text-sm font-bold text-white flex items-center gap-2">
              <Split className="w-4 h-4 text-purple-400" />
              Claim Decomposition & Entity-Relationship Modeling ({subClaims.length} Proposition{subClaims.length > 1 ? 's' : ''})
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
                    {getCentralityBadge(sub.claimCentrality)}
                    <span className="text-[11px] text-slate-400 font-mono">#{idx + 1}</span>
                  </div>
                  {getStanceBadge(sub.stance)}
                </div>

                <p className="text-sm font-medium text-white mb-2">"{sub.claimText}"</p>

                {/* Entity-Relationship-Value Triple */}
                {sub.entityRelationship && sub.entityRelationship.subject && (
                  <div className="mb-2 p-2 rounded-lg bg-slate-900/90 border border-slate-800 flex items-center gap-1.5 text-[11px] font-mono text-slate-300">
                    <span className="text-sky-300 font-semibold">{sub.entityRelationship.subject}</span>
                    <ArrowRight className="w-3 h-3 text-slate-500 shrink-0" />
                    <span className="text-amber-300 font-medium">{sub.entityRelationship.predicate}</span>
                    <ArrowRight className="w-3 h-3 text-slate-500 shrink-0" />
                    <span className="text-emerald-300 font-semibold">{sub.entityRelationship.objectValue}</span>
                  </div>
                )}

                {sub.targetMetric && (
                  <div className="mb-2 text-[11px] font-mono px-2 py-0.5 rounded bg-slate-900 border border-slate-800 text-sky-300 inline-block">
                    {sub.targetMetric}
                  </div>
                )}

                <div className="text-xs text-slate-300 flex items-center justify-between pt-2 border-t border-slate-800/80">
                  <span className="truncate pr-2">{sub.evidenceSummary}</span>
                  <span className="font-bold text-sky-400 shrink-0">{sub.claimScore != null ? `${sub.claimScore}/100` : 'N/A'}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 10. Evidence Quality & Hierarchy Matrix Table */}
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
