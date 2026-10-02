import React, { useState } from 'react';
import { 
  ShieldCheck, 
  AlertTriangle, 
  CheckCircle2, 
  HelpCircle, 
  FileText, 
  Activity, 
  Cpu, 
  Image as ImageIcon,
  Layers,
  ArrowRight,
  Info,
  AlertCircle,
  Eye,
  Sliders,
  Sparkles,
  Bot,
  Grid
} from 'lucide-react';

export default function ImageHeatmap({ imageAnalysis, uploadedImage, modalityDecision }) {
  const [forensicFilter, setForensicFilter] = useState('ela'); // 'original', 'ela', 'compression'

  if (!imageAnalysis) return null;

  const {
    imageContentType = 'NEWS_SCREENSHOT',
    textPresence = 'TEXT_PRESENT',
    rawOcrText,
    normalizedOcrText,
    reconstructedClaim,
    detectedHeadlineText,
    claimVerificationBasis = 'RECONSTRUCTED_CLAIM',
    ocrConfidence = 90,
    ocrQualityLevel = 'HIGH',
    reconstructionConfidence = 95,
    garbageCharacterRatio = 0,
    validWordRatio = 100,
    claimExtractionStatus = 'CLAIM_READY_FOR_VERIFICATION',
    forensicAssessment = 'NO_SIGNIFICANT_ANOMALY',
    manipulationVerdict,
    exifStatus = 'Stripped by Platform (Neutral)',
    compressionAssessment = 'NORMAL',
    pixelAnomalyAssessment = 'NOT_DETECTED',
    aiGenerationLikelihood = 'VERY_LOW',
    aiDetectionScore = 8.0,
    aiDetectionSignals = [],
    forensicDisclaimer = 'Forensic indicators do not independently establish that an image has been manipulated.',
    anomalyFlags = [],
    heatmapOverlayUrl
  } = imageAnalysis;

  const effectiveModality = modalityDecision || imageAnalysis.modalityDecision;

  const getModalityBadge = (mod) => {
    switch (mod) {
      case 'AUTHENTIC_MEDIA_VERIFIED_CLAIM':
        return {
          title: 'Authentic Media + Verified Claim',
          desc: 'Visual photograph/screenshot is authentic and the accompanying text claim is corroborated by accredited news archives.',
          color: 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40',
          icon: <ShieldCheck className="w-4 h-4 text-emerald-400" />
        };
      case 'MISLEADING_CONTEXT_FALSE_ATTRIBUTION':
        return {
          title: 'Authentic Media + Misleading Context (False Attribution)',
          desc: 'Visual photograph is authentic, but it has been repurposed with misleading contextual captions or recycled from a different event.',
          color: 'bg-amber-500/20 text-amber-300 border-amber-500/40',
          icon: <AlertTriangle className="w-4 h-4 text-amber-400" />
        };
      case 'SYNTHETIC_MEDIA_AUTHENTIC_EVENT':
        return {
          title: 'Synthetic / AI Media + Authentic Event',
          desc: 'Image exhibits synthetic diffusion or CGI manipulation artifacts, even though the underlying news event may have occurred.',
          color: 'bg-purple-500/20 text-purple-300 border-purple-500/40',
          icon: <Bot className="w-4 h-4 text-purple-400" />
        };
      case 'FABRICATED_MEDIA_CONTRADICTED_CLAIM':
        return {
          title: 'Fabricated Media + Contradicted Claim (Complete Hoax)',
          desc: 'Both the visual media (heavily manipulated or AI-generated) and the textual claim are demonstrably false or fabricated.',
          color: 'bg-rose-500/20 text-rose-300 border-rose-500/40',
          icon: <AlertCircle className="w-4 h-4 text-rose-400" />
        };
      default:
        return null;
    }
  };

  const modalityInfo = getModalityBadge(effectiveModality);

  const getQualityBadge = (level) => {
    switch (level?.toUpperCase()) {
      case 'HIGH':
        return <span className="px-2 py-0.5 text-xs font-bold rounded bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">OCR Quality: HIGH</span>;
      case 'MEDIUM':
        return <span className="px-2 py-0.5 text-xs font-bold rounded bg-sky-500/20 text-sky-300 border border-sky-500/30">OCR Quality: MEDIUM</span>;
      case 'LOW':
        return <span className="px-2 py-0.5 text-xs font-bold rounded bg-amber-500/20 text-amber-300 border border-amber-500/30">OCR Quality: LOW</span>;
      default:
        return <span className="px-2 py-0.5 text-xs font-bold rounded bg-rose-500/20 text-rose-300 border border-rose-500/30">OCR Quality: UNRELIABLE</span>;
    }
  };

  const getForensicBadge = (assessment) => {
    switch (assessment) {
      case 'NO_SIGNIFICANT_ANOMALY':
        return <span className="px-2.5 py-1 text-xs font-bold rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/40">No Significant Forensic Anomaly</span>;
      case 'MINOR_ANOMALIES':
        return <span className="px-2.5 py-1 text-xs font-bold rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/40">Minor Forensic Anomalies</span>;
      case 'ANOMALIES_DETECTED':
        return <span className="px-2.5 py-1 text-xs font-bold rounded-full bg-rose-500/20 text-rose-300 border border-rose-500/40">Forensic Anomalies Detected</span>;
      default:
        return <span className="px-2.5 py-1 text-xs font-bold rounded-full bg-slate-500/20 text-slate-300 border border-slate-500/40">Forensics Inconclusive</span>;
    }
  };

  const isNonClaimImage = claimExtractionStatus === 'NO_TEXT_DETECTED' || claimExtractionStatus === 'NO_CLAIM_DETECTED' || claimExtractionStatus === 'OCR_UNRELIABLE' || textPresence === 'TEXT_ABSENT';
  const displayImage = uploadedImage || heatmapOverlayUrl;

  const getFilterStyle = () => {
    switch (forensicFilter) {
      case 'ela':
        return { filter: 'contrast(175%) invert(25%) hue-rotate(190deg) brightness(1.1)' };
      case 'compression':
        return { filter: 'contrast(220%) grayscale(70%) brightness(0.95)' };
      default:
        return {};
    }
  };

  return (
    <div className="space-y-4 p-2">
      {/* Non-Claim Image Advisory Banner */}
      {isNonClaimImage && (
        <div className="p-4 rounded-xl bg-slate-900/90 border border-amber-500/30 space-y-2">
          <div className="flex items-center gap-2 text-amber-400 font-bold text-sm">
            <AlertCircle className="w-5 h-5" />
            <span>Why can't this image be verified as news?</span>
          </div>
          <p className="text-xs text-slate-200 leading-relaxed mb-0">
            {claimExtractionStatus === 'NO_TEXT_DETECTED' ? 
              "This image does not contain a sufficiently identifiable textual or factual claim (classified as a photograph or illustration). TruthLens cannot determine whether a news statement is genuine or fake from this image alone. Genuineness Score is N/A." :
              "TruthLens could not reliably extract a coherent news claim from this image due to high noise or corrupted OCR tokens. TruthLens strictly prevents inventing claims from unreadable text."}
          </p>
        </div>
      )}

      {/* Cross-Modal Decision Synthesis Banner */}
      {modalityInfo && (
        <div className={`p-4 rounded-xl border flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 ${modalityInfo.color}`}>
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-black/30 shrink-0">
              {modalityInfo.icon}
            </div>
            <div>
              <div className="text-xs font-bold uppercase tracking-wider mb-0.5">
                Cross-Modal Synthesis: {modalityInfo.title}
              </div>
              <p className="text-xs opacity-90 mb-0">
                {modalityInfo.desc}
              </p>
            </div>
          </div>
          <span className="px-2.5 py-1 text-[11px] font-mono font-bold rounded bg-black/40 border border-current shrink-0">
            {effectiveModality}
          </span>
        </div>
      )}

      {/* 1. Header with Content Type and Extraction Status */}
      <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-slate-700/50">
        <div className="flex items-center gap-2">
          <ImageIcon className="w-5 h-5 text-sky-400" />
          <span className="text-sm font-bold text-white uppercase tracking-wider">
            Image Ingestion & Forensic Layer
          </span>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-slate-800 text-slate-300 border border-slate-700">
            Type: {imageContentType.replace(/_/g, ' ')}
          </span>
          <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-sky-500/20 text-sky-300 border border-sky-500/30">
            Text Presence: {textPresence.replace(/_/g, ' ')}
          </span>
          <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-purple-500/20 text-purple-300 border border-purple-500/30">
            Status: {claimExtractionStatus.replace(/_/g, ' ')}
          </span>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
        {/* Left Column: Noise Heatmap & Forensics (5 cols) */}
        <div className="lg:col-span-5 space-y-3">
          <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 text-center">
            {displayImage ? (
              <div className="position-relative overflow-hidden rounded-lg mb-2">
                <img 
                  src={displayImage} 
                  alt="Uploaded Image Forensic Preview" 
                  className="w-full h-48 object-contain bg-black/40 rounded-lg shadow transition-all duration-300"
                  style={getFilterStyle()}
                />
              </div>
            ) : (
              <div className="w-full h-48 rounded-lg bg-slate-900 flex items-center justify-center text-slate-500 text-xs mb-2">
                No visual image payload loaded
              </div>
            )}

            {/* Filter Selector */}
            {displayImage && (
              <div className="flex items-center justify-center gap-1 mb-2">
                <button
                  type="button"
                  className={`px-2 py-0.5 text-[10px] font-semibold rounded-full border transition-all ${
                    forensicFilter === 'original' 
                      ? 'bg-sky-500/30 text-sky-300 border-sky-400' 
                      : 'bg-slate-900 text-slate-400 border-slate-700 hover:text-white'
                  }`}
                  onClick={() => setForensicFilter('original')}
                >
                  Original
                </button>
                <button
                  type="button"
                  className={`px-2 py-0.5 text-[10px] font-semibold rounded-full border transition-all ${
                    forensicFilter === 'ela' 
                      ? 'bg-purple-500/30 text-purple-300 border-purple-400' 
                      : 'bg-slate-900 text-slate-400 border-slate-700 hover:text-white'
                  }`}
                  onClick={() => setForensicFilter('ela')}
                >
                  ELA / Noise
                </button>
                <button
                  type="button"
                  className={`px-2 py-0.5 text-[10px] font-semibold rounded-full border transition-all ${
                    forensicFilter === 'compression' 
                      ? 'bg-amber-500/30 text-amber-300 border-amber-400' 
                      : 'bg-slate-900 text-slate-400 border-slate-700 hover:text-white'
                  }`}
                  onClick={() => setForensicFilter('compression')}
                >
                  Compression
                </button>
              </div>
            )}

            <span className="text-[11px] text-slate-400 font-mono block">
              {forensicFilter === 'original' ? 'Direct Uploaded Visual Preview' :
               forensicFilter === 'ela' ? 'Error Level Analysis (ELA) Noise Spectrum' :
               'Quantization Matrix & Compression Discrepancy View'}
            </span>
          </div>

          {/* Forensic Parameters Box */}
          <div className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-700/60 space-y-2 text-xs">
            <div className="flex items-center justify-between">
              <span className="text-slate-400">Forensic Assessment:</span>
              {getForensicBadge(forensicAssessment)}
            </div>
            <div className="flex items-center justify-between pt-1 border-t border-slate-800">
              <span className="text-slate-400">EXIF Metadata:</span>
              <span className="font-medium text-slate-200">{exifStatus}</span>
            </div>
            <div className="flex items-center justify-between pt-1 border-t border-slate-800">
              <span className="text-slate-400">Context Authenticity:</span>
              <span className={`font-semibold ${
                imageAnalysis.contextualAuthenticity === 'ORIGINAL_FOUND' ? 'text-emerald-400' :
                imageAnalysis.contextualAuthenticity === 'MISLEADING_CONTEXT' ? 'text-rose-400' : 'text-slate-200'
              }`}>
                {(imageAnalysis.contextualAuthenticity || 'UNVERIFIED_CONTEXT').replace(/_/g, ' ')}
              </span>
            </div>
            <div className="flex items-center justify-between pt-1 border-t border-slate-800">
              <span className="text-slate-400">Compression Profile:</span>
              <span className="font-medium text-slate-200">{compressionAssessment}</span>
            </div>
            <div className="flex items-center justify-between pt-1 border-t border-slate-800">
              <span className="text-slate-400">Pixel Inconsistencies:</span>
              <span className="font-medium text-slate-200">{pixelAnomalyAssessment}</span>
            </div>
          </div>

          {/* AI Synthetic / Diffusion Detection Box */}
          <div className="p-3.5 rounded-xl bg-purple-950/20 border border-purple-500/30 space-y-2 text-xs">
            <div className="flex items-center justify-between">
              <span className="font-bold text-purple-300 flex items-center gap-1.5 uppercase tracking-wider text-[11px]">
                <Bot className="w-3.5 h-3.5 text-purple-400" />
                AI Synthetic / Diffusion Detection
              </span>
              <span className={`px-2 py-0.5 text-[10px] font-bold rounded ${
                aiGenerationLikelihood === 'HIGH' || aiGenerationLikelihood === 'SYNTHETIC_DIFFUSION_LIKELY' ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30' :
                aiGenerationLikelihood === 'MODERATE' ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30' :
                'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
              }`}>
                {aiGenerationLikelihood.replace(/_/g, ' ')}
              </span>
            </div>

            <div className="space-y-1 pt-1">
              <div className="flex justify-between text-[11px] text-slate-400">
                <span>AI Synthetic Probability Score:</span>
                <span className="font-mono font-bold text-purple-300">{aiDetectionScore}%</span>
              </div>
              <div className="w-full h-1.5 rounded-full bg-slate-800 overflow-hidden">
                <div 
                  className={`h-full transition-all duration-500 ${
                    aiDetectionScore > 60 ? 'bg-rose-500' : aiDetectionScore > 30 ? 'bg-amber-500' : 'bg-emerald-500'
                  }`}
                  style={{ width: `${Math.min(100, Math.max(0, aiDetectionScore))}%` }}
                />
              </div>
            </div>

            {aiDetectionSignals && aiDetectionSignals.length > 0 && (
              <div className="pt-2 border-t border-purple-500/20 space-y-1">
                <span className="text-[10px] uppercase font-bold text-slate-400 block">Forensic Artifact Signals:</span>
                {aiDetectionSignals.map((sig, idx) => (
                  <div key={idx} className="flex items-center gap-1.5 text-[11px] text-slate-300">
                    <span className="w-1 h-1 rounded-full bg-purple-400 shrink-0"></span>
                    <span>{sig}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Three-Tier Text & OCR Quality (7 cols) */}
        <div className="lg:col-span-7 space-y-3">
          {/* OCR Quality Metrics Card */}
          <div className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-700/60 space-y-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Cpu className="w-4 h-4 text-sky-400" />
                <span className="text-xs font-bold uppercase tracking-wider text-slate-300">OCR & Claim Confidence Gate</span>
              </div>
              {getQualityBadge(ocrQualityLevel)}
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-1">
              <div className="p-2 rounded bg-slate-950/60 border border-slate-800 text-center">
                <span className="text-[10px] text-slate-400 block uppercase">Text Confidence</span>
                <span className="text-sm font-mono font-bold text-sky-400">{imageAnalysis.ocrTextConfidence || ocrConfidence}%</span>
              </div>
              <div className="p-2 rounded bg-slate-950/60 border border-slate-800 text-center">
                <span className="text-[10px] text-slate-400 block uppercase">Claim Confidence</span>
                <span className="text-sm font-mono font-bold text-purple-400">{imageAnalysis.centralClaimConfidence != null ? `${imageAnalysis.centralClaimConfidence}%` : 'N/A'}</span>
              </div>
              <div className="p-2 rounded bg-slate-950/60 border border-slate-800 text-center">
                <span className="text-[10px] text-slate-400 block uppercase">Valid Words</span>
                <span className="text-sm font-mono font-bold text-emerald-400">{validWordRatio}%</span>
              </div>
              <div className="p-2 rounded bg-slate-950/60 border border-slate-800 text-center">
                <span className="text-[10px] text-slate-400 block uppercase">Noise Scrubbed</span>
                <span className="text-sm font-mono font-bold text-amber-400">{imageAnalysis.ocrNoiseTokensRemoved || 0} tokens</span>
              </div>
            </div>
          </div>

          {/* Three-Tier Text Representation */}
          <div className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-700/60 space-y-2.5 text-xs">
            <div className="flex items-center justify-between">
              <span className="font-bold uppercase tracking-wider text-slate-300 flex items-center gap-1.5">
                <FileText className="w-4 h-4 text-purple-400" />
                Multi-Tier Text Representation
              </span>
              <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-purple-500/20 text-purple-300 border border-purple-500/30">
                Basis: {claimVerificationBasis.replace(/_/g, ' ')}
              </span>
            </div>

            {/* Reconstructed Claim */}
            {reconstructedClaim ? (
              <div className="p-2.5 rounded-lg bg-slate-950/70 border border-slate-800">
                <span className="text-[10px] uppercase font-bold text-sky-400 block mb-0.5">
                  Reconstructed Claim Proposition {reconstructionConfidence ? `(${reconstructionConfidence}% match)` : ''}:
                </span>
                <p className="text-xs text-white font-medium mb-0">
                  "{reconstructedClaim}"
                </p>
              </div>
            ) : null}

            {/* Normalized Text */}
            {normalizedOcrText && (
              <div className="p-2 rounded-lg bg-slate-950/40 border border-slate-800/80 text-[11px]">
                <span className="text-[10px] uppercase font-bold text-slate-400 block mb-0.5">Normalized Text:</span>
                <span className="text-slate-300 font-mono">"{normalizedOcrText}"</span>
              </div>
            )}

            {/* Raw OCR */}
            {rawOcrText ? (
              <div className="p-2 rounded-lg bg-slate-950/40 border border-slate-800/80 text-[11px]">
                <span className="text-[10px] uppercase font-bold text-slate-500 block mb-0.5">Raw OCR Stream:</span>
                <span className="text-slate-400 font-mono truncate block">"{rawOcrText}"</span>
              </div>
            ) : (
              <p className="text-xs text-slate-400 italic mb-0">No textual news stream detected in this image.</p>
            )}
          </div>

          {/* Forensic Artifact Flags */}
          {anomalyFlags && anomalyFlags.length > 0 && (
            <div className="p-3 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1.5">
              <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400 block">
                Forensic Indicator Observations
              </span>
              <div className="space-y-1 text-xs text-slate-300">
                {anomalyFlags.map((flag, idx) => (
                  <div key={idx} className="flex items-center gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-sky-400 shrink-0"></span>
                    <span>{flag}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Forensic Non-Accusatory Disclaimer */}
      <div className="p-2.5 rounded-xl bg-slate-950/80 border border-slate-800/80 flex items-center gap-2 text-xs text-slate-400">
        <Info className="w-4 h-4 text-sky-400 shrink-0" />
        <span>
          <strong>Disclaimer:</strong> {forensicDisclaimer}
        </span>
      </div>
    </div>
  );
}
