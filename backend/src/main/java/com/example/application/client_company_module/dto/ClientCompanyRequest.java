package com.example.application.client_company_module.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public class ClientCompanyRequest {
    /** Optional. If left blank, the backend auto-generates the next sequential code (e.g. CLI0001, CLI0002...). */
    private String companyCode;

    @NotBlank(message = "Company name is required")
    private String companyName;

    private String legalName;
    private String email;
    private String phone;
    private String alternatePhone;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private String contactPersonName;
    private String contactPersonEmail;
    private String contactPersonPhone;

    /** Mirrors the Employee "Enable Login" pattern: optional Client Admin login created atomically. */
    private boolean createClientAdminLogin = false;

    @Valid
    private ClientAdminLoginRequest clientAdminLogin;

    public String getCompanyCode() { return companyCode; }
    public void setCompanyCode(String companyCode) { this.companyCode = companyCode; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAlternatePhone() { return alternatePhone; }
    public void setAlternatePhone(String alternatePhone) { this.alternatePhone = alternatePhone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public String getContactPersonName() { return contactPersonName; }
    public void setContactPersonName(String contactPersonName) { this.contactPersonName = contactPersonName; }
    public String getContactPersonEmail() { return contactPersonEmail; }
    public void setContactPersonEmail(String contactPersonEmail) { this.contactPersonEmail = contactPersonEmail; }
    public String getContactPersonPhone() { return contactPersonPhone; }
    public void setContactPersonPhone(String contactPersonPhone) { this.contactPersonPhone = contactPersonPhone; }
    public boolean isCreateClientAdminLogin() { return createClientAdminLogin; }
    public void setCreateClientAdminLogin(boolean createClientAdminLogin) { this.createClientAdminLogin = createClientAdminLogin; }
    public ClientAdminLoginRequest getClientAdminLogin() { return clientAdminLogin; }
    public void setClientAdminLogin(ClientAdminLoginRequest clientAdminLogin) { this.clientAdminLogin = clientAdminLogin; }
}
