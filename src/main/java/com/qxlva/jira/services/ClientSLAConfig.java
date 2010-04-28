/**
 * Copyright (c) Quicksilva, 2010
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.qxlva.jira.services;

import java.util.Arrays;

import org.apache.commons.lang.ObjectUtils;

/**
 * @author Adrian Pillinger
 *
 */
public class ClientSLAConfig {
	private String clientName;
	private String[] projects;
	private String p1ResponseSLA;
	private String p2ResponseSLA;
	private String p3ResponseSLA;
	private String p1FixKPI;
	private String p2FixKPI;
	private String p3FixKPI;
	private int slaOperatingHours;
	
	public ClientSLAConfig(String clientName, String[] projects, String p1ResponseSLA, 
			String p2ResponseSLA, String p3ResponseSLA, String p1FixKPI, String p2FixKPI,
			String p3FixKPI, int slaOperatingHours) {
		
		setClientName(clientName);
		setProjects(projects);
		setP1ResponseSLA(p1ResponseSLA);
		setP2ResponseSLA(p2ResponseSLA);
		setP3ResponseSLA(p3ResponseSLA);
		setP1FixKPI(p1FixKPI);
		setP2FixKPI(p2FixKPI);
		setP3FixKPI(p3FixKPI);
		setSlaOperatingHours(slaOperatingHours);
	}
	
	public String getClientName() {
		return clientName;
	}
	public void setClientName(String clientName) {
		this.clientName = clientName;
	}
	public String[] getProjects() {
		return projects;
	}
	public void setProjects(String[] projects) {
		this.projects = projects;
	}
	public String getP1ResponseSLA() {
		return p1ResponseSLA;
	}
	public void setP1ResponseSLA(String responseSLA) {
		p1ResponseSLA = responseSLA;
	}
	public String getP2ResponseSLA() {
		return p2ResponseSLA;
	}
	public void setP2ResponseSLA(String responseSLA) {
		p2ResponseSLA = responseSLA;
	}
	public String getP3ResponseSLA() {
		return p3ResponseSLA;
	}
	public void setP3ResponseSLA(String responseSLA) {
		p3ResponseSLA = responseSLA;
	}
	public String getP1FixKPI() {
		return p1FixKPI;
	}
	public void setP1FixKPI(String fixKPI) {
		p1FixKPI = fixKPI;
	}
	public String getP2FixKPI() {
		return p2FixKPI;
	}
	public void setP2FixKPI(String fixKPI) {
		p2FixKPI = fixKPI;
	}
	public String getP3FixKPI() {
		return p3FixKPI;
	}
	public void setP3FixKPI(String fixKPI) {
		p3FixKPI = fixKPI;
	}
	public int getSlaOperatingHours() {
		return slaOperatingHours;
	}
	public void setSlaOperatingHours(int slaOperatingHours) {
		this.slaOperatingHours = slaOperatingHours;
	}
	
	/* The ClientSLAConfig objects are equal if they are for the same client and projects
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof ClientSLAConfig)
		{
			ClientSLAConfig cObj = (ClientSLAConfig)obj;
			final boolean isSameClient = ObjectUtils.equals(this.clientName, cObj.clientName);
			final boolean isSameProjs = Arrays.equals(this.projects, cObj.projects);
			return isSameClient && isSameProjs;
		}
		else
		{
			return super.equals(obj);
		}
	}
	
	public int hashCode() {
		String hashStr = "" + clientName;
		for (int i = 0; i < projects.length; i++)
		{
			hashStr += projects[i] == null ? "" : projects[i];
		}
		return hashStr.hashCode();
	}
}
