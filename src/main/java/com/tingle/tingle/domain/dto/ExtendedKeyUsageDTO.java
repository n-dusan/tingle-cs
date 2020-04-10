package com.tingle.tingle.domain.dto;

import java.util.List;

import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.KeyPurposeId;

public class ExtendedKeyUsageDTO {

	private boolean critical;

	private boolean clientAuth;
	private boolean codeSigning;
	private boolean emailProtection;
	private boolean ocspSigning;
	private boolean serverAuth;
	private boolean timeStamping;

	public ExtendedKeyUsageDTO() {
	}

	public void setUsages(List<String> extendedKeyUsageList) throws Exception {
		if (extendedKeyUsageList == null) {
			return;
		}
		for (int i = 0; i < extendedKeyUsageList.size(); i++) {
			switch (extendedKeyUsageList.get(i)) {
			case "1.3.6.1.5.5.7.3.1":
				this.serverAuth = true;
				break;
			case "1.3.6.1.5.5.7.3.2":
				this.clientAuth = true;
				break;
			case "1.3.6.1.5.5.7.3.3":
				this.codeSigning = true;
				break;
			case "1.3.6.1.5.5.7.3.4":
				this.emailProtection = true;
				break;
			case "1.3.6.1.5.5.7.3.8":
				this.timeStamping = true;
				break;
			case "1.3.6.1.5.5.7.3.9":
				this.ocspSigning = true;
				break;
			default:
				throw new Exception();
			}
		}

	}

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
