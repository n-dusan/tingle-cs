package com.tingle.tingle.domain.dto;

public class ExtendedKeyUsageDTO {

	private boolean critical;
	
	private boolean clientAuth;
	private boolean codeSigning;
	private boolean emailProtection;
	private boolean ocspSigning;
	private boolean serverAuth;
	private boolean timeStamping;
	
	public ExtendedKeyUsageDTO() {}

	public boolean isCritical() {
		return critical;
	}

	public void setCritical(boolean critical) {
		this.critical = critical;
	}

	public boolean isClientAuth() {
		return clientAuth;
	}

	public void setClientAuth(boolean clientAuth) {
		this.clientAuth = clientAuth;
	}

	public boolean isCodeSigning() {
		return codeSigning;
	}

	public void setCodeSigning(boolean codeSigning) {
		this.codeSigning = codeSigning;
	}

	public boolean isEmailProtection() {
		return emailProtection;
	}

	public void setEmailProtection(boolean emailProtection) {
		this.emailProtection = emailProtection;
	}

	public boolean isOcspSigning() {
		return ocspSigning;
	}

	public void setOcspSigning(boolean ocspSigning) {
		this.ocspSigning = ocspSigning;
	}

	public boolean isServerAuth() {
		return serverAuth;
	}

	public void setServerAuth(boolean serverAuth) {
		this.serverAuth = serverAuth;
	}

	public boolean isTimeStamping() {
		return timeStamping;
	}

	public void setTimeStamping(boolean timeStamping) {
		this.timeStamping = timeStamping;
	}
	
	
	
}
