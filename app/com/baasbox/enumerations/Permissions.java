package com.baasbox.enumerations;

public enum Permissions {
	ALLOW ("_allow"),
    ALLOW_READ ("_allowRead"),
    ALLOW_UPDATE ("_allowUpdate"),
    ALLOW_DELETE ("_allowDelete"),
	FULL_ACCESS ("_allow");
    
    private Permissions(String name)
    {
        this.name = name;
    }
    
    public String toString()
    {
        return name;
    }
    
    private String name;
    
}