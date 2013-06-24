package com.baasbox.db.hook;

import com.orientechnologies.orient.core.hook.ORecordHookAbstract;

public abstract class BaasBoxHook extends ORecordHookAbstract {

	public abstract String getHookName();
}
