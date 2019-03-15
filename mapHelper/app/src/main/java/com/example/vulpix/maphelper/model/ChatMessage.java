/**
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.vulpix.maphelper.model;

import com.mapbox.mapboxsdk.geometry.LatLng;

/**
 * Models for ChatActivity
 */

public class ChatMessage {

    private String id;
    private String text;
    private String name;
    private String uid;
    private String photoUrl;
    private String imageUrl;
    private LatLng destination;
    private String messageTime;

    public ChatMessage() {
    }

    public ChatMessage(String text, String name, String uid, String photoUrl, String imageUrl, LatLng destination, String messageTime) {
        this.text = text;
        this.name = name;
        this.uid = uid;
        this.photoUrl = photoUrl;
        this.imageUrl = imageUrl;
        this.destination = destination;
        this.messageTime= messageTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUid() { return uid; }

    public void setUid(String uid) { this.uid = uid; }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public LatLng getDestination() { return destination; }

    public void setDestination(LatLng destination) { this.destination = destination; }

    public String getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(String messageTime) {
        this.messageTime = messageTime;
    }

}

