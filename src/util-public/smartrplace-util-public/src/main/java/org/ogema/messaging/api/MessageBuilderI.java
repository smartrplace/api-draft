/**
 * ﻿Copyright 2020 Smartrplace UG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ogema.messaging.api;

import java.io.IOException;

/**
 *
 * @author jlapp
 */
public interface MessageBuilderI {

    public MessageBuilderI withSender(String address);

    /**
     * @param address the email address
     * @param name user's name
     * @return this builder 
     */
    public MessageBuilderI withSender(String address, String name);
    
    public MessageBuilderI withSubject(String subject);

    public MessageBuilderI addTo(String address);

    /**
     * @param address the email address
     * @param name user's name
     * @return this builder 
     */
    public MessageBuilderI addTo(String address, String name);
    
    public MessageBuilderI addCc(String address);

    /**
     * @param address the email address
     * @param name user's name
     * @return this builder 
     */
    public MessageBuilderI addCc(String address, String name);

    public MessageBuilderI addBcc(String address);

    /**
     * @param address the email address
     * @param name user's name
     * @return this builder 
     */
    public MessageBuilderI addBcc(String address, String name);

    public MessageBuilderI addText(String txt);

    public MessageBuilderI addHtml(String txt);

    /**
     * Sends this message on the session it was created with.
     * 
     * @throws IOException in case of mail messaging failures
     * @throws IllegalArgumentException if no sender address has been set,
     *         the message is empty, or has no recipients
     */
    public void send() throws IOException;
}
