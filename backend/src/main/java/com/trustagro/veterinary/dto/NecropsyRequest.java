package com.trustagro.veterinary.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class NecropsyRequest {
    private Long flockId;
    private Integer numChickensExamined;
    private String heartFindings;
    private String liverFindings;
    private String spleenFindings;
    private String lungFindings;
    private String gizzardFindings;
    private String intestineFindings;
    private String generalFindings;
    private String suspectedCauseOfDeath;
    private String finalDiagnosis;
    private String recommendations;
    private String photoUrls;

    public Long getFlockId() { return flockId; }
    public Integer getNumChickensExamined() { return numChickensExamined; }
    public String getHeartFindings() { return heartFindings; }
    public String getLiverFindings() { return liverFindings; }
    public String getSpleenFindings() { return spleenFindings; }
    public String getLungFindings() { return lungFindings; }
    public String getGizzardFindings() { return gizzardFindings; }
    public String getIntestineFindings() { return intestineFindings; }
    public String getGeneralFindings() { return generalFindings; }
    public String getSuspectedCauseOfDeath() { return suspectedCauseOfDeath; }
    public String getFinalDiagnosis() { return finalDiagnosis; }
    public String getRecommendations() { return recommendations; }
    public String getPhotoUrls() { return photoUrls; }
}
