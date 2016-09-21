/*
 * Copyright (C) 2014 RetailMeNot, Inc.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */

package com.rmn.qa;

public class NodesCouldNotBeStartedException extends Exception {
    private static final long serialVersionUID = 1L;

    public NodesCouldNotBeStartedException(String msg) {
        super(msg);
    }

    public NodesCouldNotBeStartedException(String msg, Throwable t) {
        super(msg, t);
    }
}
