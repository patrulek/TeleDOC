package com.pp.iwm.teledoc.objects;

import java.util.List;

import com.pp.iwm.teledoc.network.User;

public class Conference {	// TODO struktura, a nie obiekt
	
	// ==================================
	// FIELDS
	// ==================================
	
	private String title;
	private String description;
	private User chairman;
	private List<User> members;
	private String chairman_str;			// TODO temp solution
	// private List<ImageHistory> images;
	private boolean is_open;
	
	// ====================================
	// METHODS 
	// ====================================
	
	public Conference(String _title, String _description, List<User> _members, String _chairman, boolean _is_open) {
		title = _title;
		description = _description;
		members = _members;
		chairman_str = _chairman;
		is_open = _is_open;
	}
	
	
	public String getTitle() {
		return title;
	}
	
	public String getDescription() {
		return description;
	}
	
	public boolean isOpen() {
		return is_open;
	}

	public List<User> getMembers() {
		return members;
	}

	public User getChairman() {
		return chairman;
	}
	
	public String getChairmanStr() {
		return chairman_str;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((chairman == null) ? 0 : chairman.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((members == null) ? 0 : members.hashCode());
		result = prime * result + (is_open ? 1231 : 1237);
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		
		if (obj == null)
			return false;
		
		if (getClass() != obj.getClass())
			return false;
		
		Conference other = (Conference) obj;
		
		if (chairman == null) {
			if (other.chairman != null)
				return false;
		} else if (!chairman.equals(other.chairman))
			return false;
		
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		
		if (members == null) {
			if (other.members != null)
				return false;
		} else if (!members.equals(other.members))
			return false;
		
		if (is_open != other.is_open)
			return false;
		
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		
		return true;
	}
}
