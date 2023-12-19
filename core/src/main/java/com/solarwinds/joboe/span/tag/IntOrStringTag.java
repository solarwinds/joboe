/*
 * Copyright 2016-2018 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.solarwinds.joboe.span.tag;

import com.solarwinds.joboe.span.Span;

public class IntOrStringTag extends IntTag {
    public IntOrStringTag(String key) {
        super(key);
    }

    public void set(Span span, String tagValue) {
        span.setTag(super.key, tagValue);
    }
}