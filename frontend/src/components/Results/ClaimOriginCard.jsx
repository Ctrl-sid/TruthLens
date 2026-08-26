import React from 'react';
import { 
  Compass, 
  Building2, 
  ExternalLink, 
  Clock, 
  GitFork, 
  Layers, 
  AlertOctagon, 
  CheckCircle, 
  ShieldAlert, 
  HelpCircle,
  Share2
} from 'lucide-react';

export default function ClaimOriginCard({ originDiscovery, userClaim, evidenceClusters = [] }) {
  if (!originDiscovery) return null;

  const {
    originalPublisher,
    originalDomain,
    originalHeadline,
    originalUrl,
    publishedDate,
    provenanceType,
    provenanceStatus,
    evidenceTier,
    distortionAnalysis,
    crossReferencedConsensus,
    originMatchConfidence
  } = originDiscovery;

  const getProvenanceBadge = (type) => {
    switch (type) {
      case 'AUTHENTIC_REPRODUCTION':
        return {
          label: 'Authentic News Reproduction',
          badgeClass: 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40',
          icon: <CheckCircle className="w-4 h-4 text-emerald-400" />
        };
      case 'ALTERED_DISTORTION':
        return {
          label: 'Altered / Distorted Headline',
          badgeClass: 'bg-rose-500/20 text-rose-300 border-rose-500/40',
          icon: <AlertOctagon className="w-4 h-4 text-rose-400" />
        };
      case 'DOCUMENTED_HOAX':
        return {
          label: 'Documented Viral Hoax',
          badgeClass: 'bg-rose-500/25 text-rose-300 border-rose-500/50',
          icon: <ShieldAlert className="w-4 h-4 text-rose-400" />
        };
      case 'UNVERIFIED_ORIGIN':
      default:
        return {
          label: 'Unverified Online Claim',
          badgeClass: 'bg-amber-500/20 text-amber-300 border-amber-500/40',
          icon: <HelpCircle className="w-4 h-4 text-amber-400" />
        };
    }
  };

  const getTierBadge = (tier) => {
    const t = tier || 'LEVEL_2_SECONDARY';
    if (t.includes('LEVEL_1')) return <span className="px-2.5 py-0.5 text-xs font-bold rounded-full bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">Level 1 Primary Gov / Official</span>;
    if (t.includes('LEVEL_3')) return <span className="px-2.5 py-0.5 text-xs font-bold rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30">Level 3 Fact Check Database</span>;
    if (t.includes('LEVEL_4')) return <span className="px-2.5 py-0.5 text-xs font-bold rounded-full bg-cyan-500/20 text-cyan-300 border border-cyan-500/30">Level 4 Reference Archive</span>;
    if (t.includes('LEVEL_5')) return <span className="px-2.5 py-0.5 text-xs font-bold rounded-full bg-rose-500/20 text-rose-300 border border-rose-500/30">Level 5 User Social / Forum</span>;
    return <span className="px-2.5 py-0.5 text-xs font-bold rounded-full bg-sky-500/20 text-sky-300 border border-sky-500/30">Level 2 Accredited News Wire</span>;
  };

  const badgeInfo = getProvenanceBadge(provenanceType);

  return (
    <div className="space-y-6">
      {/* 1. Header & Provenance Classification */}
      <div className="p-5 rounded-2xl bg-gradient-to-r from-slate-900/90 via-slate-800/80 to-slate-900/90 border border-slate-700/60 shadow-xl backdrop-blur-md">
        <div className="flex flex-wrap items-center justify-between gap-3 pb-4 border-b border-slate-700/50">
          <div>
            <span className="text-xs uppercase tracking-wider font-bold text-sky-400">Provenance Radar</span>
            <h3 className="text-lg font-bold text-white mt-0.5 flex items-center gap-2">
              <Compass className="w-5 h-5 text-sky-400" />
              Source Provenance & Origin Discovery
            </h3>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            {getTierBadge(evidenceTier)}
            <span className={`px-3 py-1 text-xs font-bold rounded-full border flex items-center gap-1.5 ${badgeInfo.badgeClass}`}>
              {badgeInfo.icon}
              <span>{badgeInfo.label}</span>
            </span>
          </div>
        </div>

        {/* 2. Technical Provenance Radar Flow Graph */}
        <div className="mt-4 p-4 rounded-xl bg-slate-950/60 border border-slate-700/50">
          <div className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3 flex items-center gap-1.5">
            <GitFork className="w-4 h-4 text-sky-400" />
            Provenance Graph Flow (Claim $\rightarrow$ Clusters $\rightarrow$ Verdict)
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 relative">
            {/* Node 1: Submitted Claim */}
            <div className="p-3 rounded-lg bg-slate-900/90 border border-slate-700/70 text-center">
              <span className="text-[11px] uppercase font-bold text-slate-400 block mb-1">User Prompt</span>
              <p className="text-xs text-white font-medium truncate" title={userClaim || 'User Input'}>
                "{userClaim || 'Analyzed News Claim'}"
              </p>
            </div>

            {/* Node 2: Origin & Evidence Cluster */}
            <div className="p-3 rounded-lg bg-sky-950/40 border border-sky-500/40 text-center">
              <span className="text-[11px] uppercase font-bold text-sky-300 block mb-1">
                {provenanceStatus ? provenanceStatus.replace(/_/g, ' ') : 'Primary Wire Cluster'}
              </span>
              <p className="text-xs text-sky-200 font-semibold truncate">
                {originalPublisher || 'Accredited Wire Network'}
              </p>
            </div>

            {/* Node 3: Provenance Verdict */}
            <div className={`p-3 rounded-lg border text-center ${
              provenanceType === 'AUTHENTIC_REPRODUCTION' ? 'bg-emerald-950/40 border-emerald-500/40 text-emerald-300' :
              provenanceType === 'ALTERED_DISTORTION' ? 'bg-rose-950/40 border-rose-500/40 text-rose-300' :
              'bg-amber-950/40 border-amber-500/40 text-amber-300'
            }`}>
              <span className="text-[11px] uppercase font-bold block mb-1">Provenance Match</span>
              <span className="text-xs font-bold">{badgeInfo.label}</span>
            </div>
          </div>
        </div>

        {/* 3. Primary Originating Publisher Details */}
        <div className="mt-4 p-4 rounded-xl bg-slate-900/80 border border-slate-700/60">
          <div className="flex flex-wrap items-center justify-between gap-3 mb-3">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-sky-500/10 border border-sky-500/30 flex items-center justify-center text-sky-400">
                <Building2 className="w-5 h-5" />
              </div>
              <div>
                <span className="text-[11px] uppercase font-bold text-slate-400 block tracking-wider">
                  Primary Originating Publisher
                </span>
                <span className="text-base font-bold text-white">
                  {originalPublisher || 'Unverified Web'}
                </span>
              </div>
            </div>

            <div className="flex items-center gap-2">
              {publishedDate && (
                <span className="px-2.5 py-1 text-xs rounded-lg bg-slate-800 text-slate-300 border border-slate-700 flex items-center gap-1.5">
                  <Clock className="w-3.5 h-3.5 text-slate-400" />
                  {publishedDate}
                </span>
              )}
              {originMatchConfidence > 0 && (
                <span className="px-2.5 py-1 text-xs font-mono font-bold rounded-lg bg-sky-500/20 text-sky-300 border border-sky-500/30">
                  Match: {originMatchConfidence}%
                </span>
              )}
            </div>
          </div>

          {/* Original Headline */}
          {originalHeadline && (
            <div className="mt-2 p-3 rounded-lg bg-slate-950/70 border border-slate-800/90">
              <span className="text-[11px] uppercase font-bold text-slate-400 block mb-1">
                Original Published Headline / Archive Record:
              </span>
              <p className="text-sm font-semibold text-slate-200 italic">
                "{originalHeadline}"
              </p>
              {originalUrl && (
                <a
                  href={originalUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="mt-2 text-xs font-medium text-sky-400 hover:text-sky-300 inline-flex items-center gap-1 transition-colors"
                >
                  <span>View original report at {originalDomain || 'source'}</span>
                  <ExternalLink className="w-3.5 h-3.5" />
                </a>
              )}
            </div>
          )}
        </div>

        {/* 4. Distortion & Provenance Rationale */}
        {distortionAnalysis && (
          <div className="mt-4 p-4 rounded-xl bg-slate-900/60 border border-slate-700/50">
            <h4 className="text-xs font-bold uppercase tracking-wider text-sky-400 mb-1.5 flex items-center gap-1.5">
              <Share2 className="w-4 h-4" />
              Provenance & Distortion Analysis
            </h4>
            <p className="text-sm text-slate-200 leading-relaxed">
              {distortionAnalysis}
            </p>
          </div>
        )}

        {/* 5. Cross-Referenced Consensus Summary */}
        {crossReferencedConsensus && (
          <div className="mt-4 p-3 rounded-xl bg-sky-950/30 border border-sky-500/20 flex items-center gap-2.5 text-sm text-slate-200">
            <Layers className="w-4 h-4 text-sky-400 shrink-0" />
            <div>
              <span className="font-semibold text-sky-400">Consensus: </span>
              <span>{crossReferencedConsensus}</span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
