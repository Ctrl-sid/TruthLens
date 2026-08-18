package com.truthlens.api.nlp;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FactCheckingCorpus {

    private final TfIdfVectoriser tfIdfVectoriser;
    private final CosineSimilarityEngine cosineSimilarityEngine;

    @Getter
    @Builder
    public static class CorpusEntry {
        private String id;
        private String text;
        private String category;
        private boolean isDebunkedFake;
        private String verdictRating;
        private String sourceName;
        private String sourceDomain;
        private String sourceUrl;
        private String articleTitle;
        private String rationale;
        private List<String> keywords;
    }

    @Getter
    @Builder
    public static class MatchResult {
        private CorpusEntry bestDebunkedEntry;
        private double debunkedSimilarity;

        private CorpusEntry bestVerifiedEntry;
        private double verifiedSimilarity;

        private CorpusEntry topOverallEntry;
        private double topSimilarity;
    }

    private final List<CorpusEntry> corpus = new ArrayList<>();
    private List<String> allCorpusTexts = new ArrayList<>();

    @PostConstruct
    public void init() {
        populateDebunkedClaims();
        populateVerifiedFacts();
        this.allCorpusTexts = corpus.stream().map(CorpusEntry::getText).collect(Collectors.toList());
    }

    private void populateDebunkedClaims() {
        // Health & Medical Hoaxes
        addEntry("HOAX-001",
                "Boiling lemon water with baking soda cures all forms of cancer and eliminates tumors in 48 hours without chemotherapy",
                "Health Misinformation", true, "Debunked / False",
                "Snopes Fact Check", "snopes.com", "https://www.snopes.com/fact-check/lemon-water-cancer-cure/",
                "Fact Check: Can Lemon Water and Baking Soda Cure Cancer?",
                "Extensive clinical oncological trials show citrus and alkaline mixtures cannot selectively eradicate malignant cancer tumors. Delaying medical chemotherapy for unproven remedies is medically dangerous.",
                List.of("lemon", "baking", "soda", "cancer", "cure", "chemotherapy", "tumor"));

        addEntry("HOAX-002",
                "Drinking bleach or chlorine dioxide miracle mineral solution completely cures autism, coronavirus, and all infectious diseases",
                "Health Misinformation", true, "Debunked / False",
                "FDA & PolitiFact", "politifact.com", "https://www.politifact.com/factchecks/miracle-mineral-solution-bleach/",
                "FDA Consumer Warning: Miracle Mineral Solution is Industrial Bleach",
                "Health agencies worldwide (FDA, CDC, WHO) warn that consuming industrial bleaching chemicals causes severe gastrointestinal damage, organ failure, and death.",
                List.of("bleach", "chlorine", "miracle", "cure", "autism", "virus", "infection"));

        addEntry("HOAX-003",
                "Eating raw garlic, ginger, and onions completely reverses diabetes in 7 days without insulin or medical treatment",
                "Health Misinformation", true, "Debunked / False",
                "Reuters Fact Check", "reuters.com", "https://www.reuters.com/fact-check/garlic-diabetes-cure/",
                "Fact Check: Raw Garlic and Ginger Do Not Cure Diabetes in 7 Days",
                "Endocrinologists confirm Type 1 and Type 2 diabetes require proper glucose monitoring, lifestyle adjustments, and prescribed insulin/medication. Dietary herbs cannot regenerate pancreatic beta cells.",
                List.of("garlic", "ginger", "diabetes", "insulin", "cure", "reverses", "pancreas"));

        addEntry("HOAX-004",
                "Secret miracle cure for all diseases discovered by anonymous doctor but suppressed by big pharma and governments",
                "Health Misinformation", true, "Debunked / False",
                "Associated Press", "apnews.com", "https://apnews.com/ap-fact-check",
                "AP Fact Check: Phantom 'Secret Cures' Suppressed by Big Pharma",
                "Sensational claims alleging a single universal panacea for all diseases are mathematically and biologically fraudulent marketing traps.",
                List.of("secret", "miracle", "cure", "anonymous", "doctor", "big", "pharma", "suppressed"));

        // Technology & 5G & Telecommunications
        addEntry("HOAX-005",
                "5G mobile wireless towers emit radiation that creates COVID-19 viruses and controls human brains",
                "Technology & Conspiracy", true, "Debunked / False",
                "Full Fact", "fullfact.org", "https://fullfact.org/online/5g-coronavirus-hoax/",
                "Full Fact Investigation: 5G Radio Frequency and Viral Biology",
                "Radio frequency electromagnetic radiation in the non-ionizing spectrum cannot create biological organisms like viruses. COVID-19 spread in countries with zero 5G infrastructure.",
                List.of("5g", "towers", "radiation", "virus", "covid", "brains", "cellular"));

        addEntry("HOAX-006",
                "COVID-19 and flu vaccines contain microscopic tracking microchips and nanobots for government surveillance",
                "Health & Conspiracy", true, "Pants on Fire / False",
                "PolitiFact", "politifact.com", "https://www.politifact.com/factchecks/vaccine-microchips-surveillance/",
                "PolitiFact: No Microchips or Tracking Devices in Vaccines",
                "Vaccine composition is publicly audited by international regulators (FDA, EMA, WHO). No microchip technology exists at liquid nano-scale for injectable wireless tracking.",
                List.of("vaccines", "microchips", "tracking", "nanobots", "surveillance", "injected", "chips"));

        // Financial Scams & Debt Relief
        addEntry("HOAX-007",
                "World Bank and Federal Reserve announced all national personal debts and mortgages will be wiped clean by Friday",
                "Financial Misinformation", true, "Debunked / False",
                "Reuters Fact Check", "reuters.com", "https://www.reuters.com/fact-check/world-bank-debt-wipeout/",
                "Reuters Fact Check: Fabricated Claims of Global Debt Cancellation",
                "Neither the World Bank nor the Federal Reserve has enacted or proposed unilateral individual debt forgiveness or universal cash giveaways.",
                List.of("world", "bank", "federal", "reserve", "debts", "wiped", "mortgages", "forgiveness"));

        addEntry("HOAX-008",
                "Leaked audio and video of banking CEO admitting secret plan to shut down all ATMs and confiscate customer bank deposits",
                "Financial Panic Hoax", true, "Fabricated / Fake",
                "Associated Press", "apnews.com", "https://apnews.com/ap-fact-check",
                "AP Fact Check: AI-Generated Deepfake Audio Targets Banking Institutions",
                "Audio analysis confirms synthetic AI voice cloning (deepfake) intended to trigger artificial banking panic. Official regulatory reserves remain fully solvent.",
                List.of("leaked", "audio", "video", "banking", "ceo", "atms", "confiscate", "deposits", "deepfake"));

        // Conspiracy & Astronomical / Pseudoscience
        addEntry("HOAX-009",
                "Earth is flat and Antarctic ice wall is guarded by United Nations military warships to prevent exploration",
                "Pseudoscience & Conspiracy", true, "Debunked / False",
                "Snopes Fact Check", "snopes.com", "https://www.snopes.com/fact-check/flat-earth-antarctic-treaty/",
                "Snopes: Flat Earth Ice Wall and Antarctic Treaty Conspiracy Debunked",
                "Centuries of astronomical observation, satellite telemetry, circumnavigations, and orbital space missions consistently prove the Earth is an oblate spheroid.",
                List.of("flat", "earth", "antarctic", "ice", "wall", "guarded", "conspiracy"));

        addEntry("HOAX-010",
                "NASA faked the Apollo moon landings in a movie soundstage directed by Stanley Kubrick",
                "Pseudoscience & Conspiracy", true, "Debunked / False",
                "History & Reuters", "reuters.com", "https://reuters.com/fact-check/apollo-moon-landing/",
                "Fact Check: Retroreflectors and Lunar Samples Prove Apollo Moon Landings",
                "Over 380 kilograms of lunar samples, laser retroreflectors still used today, and telemetry tracked by the Soviet Union independently confirm human lunar landings.",
                List.of("nasa", "faked", "moon", "landing", "apollo", "soundstage", "kubrick"));

        addEntry("HOAX-011",
                "Commercial airplanes are spraying toxic chemtrails in the sky to modify weather and poison the population",
                "Environmental Misinformation", true, "Debunked / False",
                "FactCheck.org", "factcheck.org", "https://www.factcheck.org/chemtrails-conspiracy-theory/",
                "FactCheck.org: Aircraft Condensation Trails are Normal Ice Crystals",
                "Atmospheric scientists and aviation engineers confirm white vapor trails behind aircraft are ordinary contrails composed of condensed water vapor and ice crystals.",
                List.of("airplanes", "chemtrails", "spraying", "toxic", "weather", "poison", "population"));

        addEntry("HOAX-012",
                "Actor or prominent political figure secretly arrested and executed at military tribunal with public body double replacing them",
                "Celebrity / Political Hoax", true, "Debunked / False",
                "PolitiFact", "politifact.com", "https://www.politifact.com/factchecks/celebrity-arrest-tribunals/",
                "PolitiFact: Viral Claims of Secret Military Arrests and Body Doubles",
                "Public court records, live verifiable press conferences, and verified appearances debunk recurrent social media rumors of secret military tribunals.",
                List.of("actor", "arrested", "tribunal", "executed", "secret", "double", "treason"));

        addEntry("HOAX-013",
                "Extraterrestrial alien fleet spotted approaching Earth and entering atmosphere confirmed by world governments",
                "Sensationalist Fabricated Claim", true, "Fabricated / Fake",
                "Snopes Fact Check", "snopes.com", "https://www.snopes.com/fact-check/alien-invasion-rumors/",
                "Snopes: No Credible Astronomical Detection of Alien Fleet",
                "Global astronomical observatories (NASA, ESA, JAXA, SETI) report no anomalous extraterrestrial armadas. Viral video posts rely on CGI animations.",
                List.of("extraterrestrial", "alien", "fleet", "approaching", "earth", "invasion", "ufo"));
    }

    private void populateVerifiedFacts() {
        // Space & Astronomy
        addEntry("TRUE-001",
                "NASA James Webb Space Telescope discovers atmospheric water vapor and carbon molecules on distant exoplanet LHS 1140b",
                "Science & Space", false, "Verified True",
                "NASA & Reuters", "reuters.com", "https://www.reuters.com/science/nasa-webb-telescope-exoplanet-atmosphere/",
                "NASA James Webb Space Telescope Exoplanet Atmosphere Spectroscopy",
                "Spectroscopic transit observations by the James Webb Space Telescope confirmed signatures of atmospheric water vapor and chemical biomarkers on temperate exoplanet candidates.",
                List.of("nasa", "james", "webb", "telescope", "exoplanet", "water", "vapor", "atmosphere"));

        addEntry("TRUE-002",
                "World Health Organization publishes updated clinical guidelines on seasonal viral mitigation and vaccination schedules",
                "Public Health", false, "Verified True",
                "World Health Organization (WHO)", "who.int", "https://www.who.int/news/item/guidelines-seasonal-mitigation",
                "WHO Official News Release: Seasonal Mitigation and Immunization Framework",
                "Published by the World Health Organization technical advisory group following peer-reviewed epidemiology reviews across member states.",
                List.of("world", "health", "organization", "who", "guidelines", "vaccination", "mitigation", "research"));

        addEntry("TRUE-003",
                "Scientists at Lawrence Livermore National Laboratory achieve net energy gain fusion ignition breakthrough",
                "Energy & Physics", false, "Verified True",
                "Nature & US Dept of Energy", "nature.com", "https://www.nature.com/articles/d41586-022-04440-7",
                "Nuclear Fusion Net Energy Gain Experiment Confirmed by US National Ignition Facility",
                "Laser fusion experiments at the National Ignition Facility produced more energy output from controlled nuclear fusion than the laser energy delivered to the target.",
                List.of("scientists", "nuclear", "fusion", "net", "energy", "gain", "ignition", "breakthrough"));

        addEntry("TRUE-004",
                "European Space Agency Euclid space telescope releases first full-color wide-field high-resolution cosmos survey images",
                "Astronomy & Space", false, "Verified True",
                "European Space Agency (ESA)", "esa.int", "https://www.esa.int/Science_Exploration/Space_Science/Euclid",
                "ESA Euclid Mission Delivers Deep Universe Dark Matter Survey",
                "Official space agency data release detailing dark matter clustering, galaxy filament morphology, and cosmic evolution mapping across billions of light years.",
                List.of("european", "space", "agency", "euclid", "telescope", "images", "cosmos", "survey"));

        addEntry("TRUE-005",
                "FDA approves breakthrough CRISPR Cas9 gene editing therapeutic Casgevy for treatment of sickle cell disease",
                "Medical Science", false, "Verified True",
                "Associated Press & FDA", "apnews.com", "https://apnews.com/article/fda-crispr-gene-editing-sickle-cell",
                "FDA Approves First CRISPR Gene-Editing Therapy for Human Genetic Disease",
                "Regulatory authorization following Phase III clinical trials demonstrating long-term correction of defective hemoglobin synthesis in sickle cell patients.",
                List.of("fda", "approves", "crispr", "gene", "editing", "casgevy", "sickle", "cell"));

        addEntry("TRUE-006",
                "DeepMind AlphaFold protein structure database expands to predict 3D structures for virtually all known catalogued proteins",
                "Artificial Intelligence & Biology", false, "Verified True",
                "Nature Scientific Journal", "nature.com", "https://www.nature.com/articles/s41586-021-03819-2",
                "AlphaFold AI Solves 50-Year Biological Protein Folding Challenge",
                "Peer-reviewed computational structural biology research accelerating pharmaceutical development, enzyme engineering, and molecular genetics worldwide.",
                List.of("deepmind", "alphafold", "protein", "structure", "database", "biology", "ai"));

        addEntry("TRUE-007",
                "Oxford University and WHO approve R21 Matrix-M malaria vaccine for widespread children immunization rollout across Africa",
                "Global Health & Medicine", false, "Verified True",
                "BBC News & WHO", "bbc.com", "https://www.bbc.com/news/health-66985273",
                "WHO Recommends Oxford R21 Malaria Vaccine for High-Risk Regions",
                "Large scale Phase 3 trials in African nations showed over 75% vaccine efficacy in reducing symptomatic malaria cases among infants and young children.",
                List.of("oxford", "malaria", "vaccine", "r21", "who", "africa", "children", "immunization"));
    }

    private void addEntry(String id, String text, String category, boolean isDebunkedFake,
                          String verdictRating, String sourceName, String sourceDomain,
                          String sourceUrl, String articleTitle, String rationale, List<String> keywords) {
        corpus.add(CorpusEntry.builder()
                .id(id)
                .text(text)
                .category(category)
                .isDebunkedFake(isDebunkedFake)
                .verdictRating(verdictRating)
                .sourceName(sourceName)
                .sourceDomain(sourceDomain)
                .sourceUrl(sourceUrl)
                .articleTitle(articleTitle)
                .rationale(rationale)
                .keywords(keywords)
                .build());
    }

    public MatchResult matchClaimAgainstCorpus(String claimText) {
        if (claimText == null || claimText.isBlank()) {
            return MatchResult.builder().build();
        }

        Map<String, Double> queryVector = tfIdfVectoriser.createTfIdfVector(claimText, allCorpusTexts);
        String cleanQuery = claimText.toLowerCase();

        CorpusEntry bestDebunked = null;
        double maxDebunkedSim = 0.0;

        CorpusEntry bestVerified = null;
        double maxVerifiedSim = 0.0;

        for (CorpusEntry entry : corpus) {
            Map<String, Double> entryVector = tfIdfVectoriser.createTfIdfVector(entry.getText(), allCorpusTexts);
            double sim = cosineSimilarityEngine.computeCosineSimilarity(queryVector, entryVector);

            // Keyword boost for high-salience terms
            long keywordHits = entry.getKeywords().stream().filter(kw -> cleanQuery.contains(kw.toLowerCase())).count();
            if (keywordHits >= 3) {
                sim = Math.min(1.0, sim + 0.30);
            } else if (keywordHits >= 2) {
                sim = Math.min(1.0, sim + 0.18);
            }

            if (entry.isDebunkedFake()) {
                if (sim > maxDebunkedSim) {
                    maxDebunkedSim = sim;
                    bestDebunked = entry;
                }
            } else {
                if (sim > maxVerifiedSim) {
                    maxVerifiedSim = sim;
                    bestVerified = entry;
                }
            }
        }

        CorpusEntry topOverall = (maxDebunkedSim >= maxVerifiedSim) ? bestDebunked : bestVerified;
        double topSim = Math.max(maxDebunkedSim, maxVerifiedSim);

        return MatchResult.builder()
                .bestDebunkedEntry(bestDebunked)
                .debunkedSimilarity(maxDebunkedSim)
                .bestVerifiedEntry(bestVerified)
                .verifiedSimilarity(maxVerifiedSim)
                .topOverallEntry(topOverall)
                .topSimilarity(topSim)
                .build();
    }
}
