package com.d_m.ast;

import java.io.Serial;

public class CheckException extends Exception {
	@Serial
    private static final long serialVersionUID = 1426078121823942827L;

	public CheckException(String message) {
        super(message);
    }
}
