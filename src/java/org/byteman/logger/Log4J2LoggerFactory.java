/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.byteman.logger;

import org.apache.logging.log4j.LogManager;

public final class Log4J2LoggerFactory extends InternalLoggerFactory {

    public static final InternalLoggerFactory INSTANCE = new Log4J2LoggerFactory();

    /**
     * Use {@link #INSTANCE} instead.
     */
    private Log4J2LoggerFactory() {

    }

    @Override
    public InternalLogger newInstance(String name) {
        // 使用 log4j2 来生成 Logger 实例
        // 依赖的jar包是：log4j-api(做接口定义) 和 log4j-core(具体的实现)
        return new Log4J2Logger(LogManager.getLogger(name));
    }
}
