package com.zimperium.zanti.cracker;

import java.io.Serializable;
import java.util.List;

import com.zimperium.zanti.cracker.CrackerOptions.Protocol;


public class DefinedCrackerOption implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public List<Protocol> cracker_protocols;
	public int selected_crack_type;
	public String custom_pass_file;
	public String crack_incremental_options;
	public String Target, TargetNetwork;

}
