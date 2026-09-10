package com.example.application.subscription_module.entity;

import jakarta.persistence.*;

/** One (plan, feature code) pair the plan explicitly turns on - see FeatureCode for valid codes. No row for a given (plan, code) means that plan does not include it. */
@Entity
@Table(name = "plan_features")
public class PlanFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "feature_code", nullable = false, length = 60)
    private String featureCode;

    @Column(nullable = false)
    private boolean enabled = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public String getFeatureCode() { return featureCode; }
    public void setFeatureCode(String featureCode) { this.featureCode = featureCode; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
