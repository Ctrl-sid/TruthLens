package com.truthlens.api.nlp;

import com.truthlens.api.dto.ClaimVerificationResponse.ClaimContextInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ClaimContextService {

    private static final Pattern COUNTRY_PATTERN = Pattern.compile(
            "(?i)\\b(Nepal|India|Indian|Nepalese|United\\s+States|USA|US|UK|Britain|British|China|Chinese|Russia|Russian|Ukraine|Ukrainian|Japan|Japanese|Israel|Israeli|Gaza|Palestine|Palestinian|Bangladesh|Bangladeshi|France|French|Germany|German)\\b"
    );

    public ClaimContextInfo extractClaimContext(String text) {
        if (text == null || text.isBlank()) {
            return ClaimContextInfo.builder()
                    .domain("General Public Assertion")
                    .claimType("GENERAL_CLAIM")
                    .build();
        }

        List<String> geos = extractGeographicEntities(text);
        String domain = determineDomain(text);
        String claimType = determineClaimClass(text, domain);
        List<String> targetAuthorities = determineTargetAuthorities(geos, domain, text);

        return ClaimContextInfo.builder()
                .geographicEntities(geos)
                .domain(domain)
                .claimType(claimType)
                .targetAuthorityInstitutions(targetAuthorities)
                .build();
    }

    public List<String> extractGeographicEntities(String text) {
        List<String> list = new ArrayList<>();
        Matcher m = COUNTRY_PATTERN.matcher(text);
        while (m.find()) {
            String geo = normalizeGeoName(m.group(1));
            if (!list.contains(geo)) {
                list.add(geo);
            }
        }
        return list;
    }

    private String normalizeGeoName(String match) {
        String lower = match.toLowerCase();
        if (lower.contains("nepal")) return "Nepal";
        if (lower.contains("india")) return "India";
        if (lower.contains("us") || lower.contains("united states")) return "United States";
        if (lower.contains("uk") || lower.contains("brit")) return "United Kingdom";
        if (lower.contains("china") || lower.contains("chinese")) return "China";
        if (lower.contains("russia")) return "Russia";
        if (lower.contains("ukrain")) return "Ukraine";
        if (lower.contains("israel")) return "Israel";
        if (lower.contains("palestin") || lower.contains("gaza")) return "Palestine / Gaza";
        if (lower.contains("bangladesh")) return "Bangladesh";
        return match.substring(0, 1).toUpperCase() + match.substring(1).toLowerCase();
    }

    private String determineDomain(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("flood") || lower.contains("earthquake") || lower.contains("cyclone") || lower.contains("landslide") ||
            lower.contains("relief") || lower.contains("rescue") || lower.contains("disaster") || lower.contains("missing") || lower.contains("toll")) {
            return "Disaster Relief & Emergency Response";
        }
        if (lower.contains("nasa") || lower.contains("isro") || lower.contains("space") || lower.contains("telescope") ||
            lower.contains("launch") || lower.contains("satellite") || lower.contains("exoplanet") || lower.contains("rocket")) {
            return "Space Exploration & Astronomy";
        }
        if (lower.contains("vaccine") || lower.contains("who") || lower.contains("health") || lower.contains("fda") ||
            lower.contains("clinical") || lower.contains("disease") || lower.contains("cure") || lower.contains("therapy")) {
            return "Public Health & Medicine";
        }
        if (lower.contains("sends") || lower.contains("signed") || lower.contains("treaty") || lower.contains("minister") ||
            lower.contains("parliament") || lower.contains("government") || lower.contains("approved") || lower.contains("sanctions")) {
            return "Government Policy & International Affairs";
        }
        return "General News & Factual Claims";
    }

    private String determineClaimClass(String text, String domain) {
        String lower = text.toLowerCase();
        if (lower.contains("sends") || lower.contains("sent") || lower.contains("approved") || lower.contains("ordered") || lower.contains("dispatched")) {
            return "GOVERNMENT_ACTION";
        }
        if (lower.contains("dead") || lower.contains("died") || lower.contains("killed") || lower.contains("casualties") || lower.contains("missing")) {
            return "DISASTER_CASUALTY";
        }
        if (lower.contains("discovered") || lower.contains("found") || lower.contains("breakthrough") || lower.contains("approves")) {
            return "SCIENTIFIC_DISCOVERY";
        }
        return "EVENT_OCCURRENCE";
    }

    private List<String> determineTargetAuthorities(List<String> geos, String domain, String text) {
        List<String> authorities = new ArrayList<>();
        String lower = text.toLowerCase();

        if (geos.contains("Nepal") && domain.contains("Disaster")) {
            authorities.add("Nepal Police");
            authorities.add("National Disaster Risk Reduction and Management Authority (NDRRMA)");
            authorities.add("Ministry of Home Affairs (Nepal)");
        }
        if (geos.contains("India") && (lower.contains("relief") || lower.contains("sends") || lower.contains("assistance"))) {
            authorities.add("Ministry of External Affairs (India)");
            authorities.add("Press Information Bureau (PIB)");
            authorities.add("National Disaster Response Force (NDRF)");
        }
        if (domain.contains("Space")) {
            authorities.add("NASA");
            authorities.add("ISRO");
            authorities.add("European Space Agency (ESA)");
        }
        if (domain.contains("Health")) {
            authorities.add("World Health Organization (WHO)");
            authorities.add("FDA / CDC");
            authorities.add("Ministry of Health and Family Welfare");
        }

        if (authorities.isEmpty()) {
            authorities.add("Accredited Primary Reporting Authority");
            authorities.add("Official Government Press Dispatch");
        }

        return authorities;
    }

    public double calculateContextualAuthorityScore(String sourceName, String domain, String geographicContext, String claimDomain) {
        if (sourceName == null && domain == null) return 0.50;
        String s = (sourceName != null ? sourceName.toLowerCase() : "");
        String d = (domain != null ? domain.toLowerCase() : "");
        String g = (geographicContext != null ? geographicContext.toLowerCase() : "");

        double authority = 0.70; // baseline for accredited secondary

        // Direct primary regional emergency/police/gov authority
        if (s.contains("police") || s.contains("disaster") || s.contains("ministry") || s.contains("government") || d.contains(".gov") || d.contains(".gov.np") || d.contains(".gov.in")) {
            authority = 0.98;
        } else if (s.contains("reuters") || s.contains("associated press") || s.contains("ap news") || s.contains("pti") || s.contains("afp")) {
            authority = 0.92;
        } else if (s.contains("the hindu") || s.contains("bbc") || s.contains("indian express") || s.contains("kathmandu post") || s.contains("himalayan times")) {
            // Regional accredited newspaper with high geographic relevance
            if (g.contains("nepal") && (s.contains("kathmandu") || s.contains("himalayan") || s.contains("hindu") || s.contains("express"))) {
                authority = 0.95;
            } else {
                authority = 0.90;
            }
        }

        return authority;
    }
}
