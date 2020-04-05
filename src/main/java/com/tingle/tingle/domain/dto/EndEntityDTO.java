package com.tingle.tingle.domain.dto;

public class EndEntityDTO {

	private CertificateX500NameDTO subject;
	private CertificateX500NameDTO issuer;
	
	public EndEntityDTO() {}

	public EndEntityDTO(CertificateX500NameDTO subject, CertificateX500NameDTO issuer) {
		super();
		this.subject = subject;
		this.issuer = issuer;
	}

	public CertificateX500NameDTO getSubject() {
		return subject;
	}

	public void setSubject(CertificateX500NameDTO subject) {
		this.subject = subject;
	}

	public CertificateX500NameDTO getIssuer() {
		return issuer;
	}

	public void setIssuer(CertificateX500NameDTO issuer) {
		this.issuer = issuer;
	}
	
	
	
}
