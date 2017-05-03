package com.nivida.smartnode;

import android.app.Activity;
import com.robotium.recorder.executor.Executor;

@SuppressWarnings("rawtypes")
public class SplashActivityExecutor extends Executor {

	@SuppressWarnings("unchecked")
	public SplashActivityExecutor() throws Exception {
		super((Class<? extends Activity>) Class.forName("com.nivida.smartnode.SplashActivity"),  "com.nivida.smartnode.R.id.", new android.R.id(), false, false, "1469511172244");
	}

	public void setUp() throws Exception { 
		super.setUp();
	}
}
