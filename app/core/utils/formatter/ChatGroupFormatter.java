package core.utils.formatter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import models.ChatGroup;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;

/**
 * 返回攻略的摘要
 * <p>
 * Created by topy on 2014/11/07.
 */
public class ChatGroupFormatter extends AbstractFormatter<ChatGroup> {

    public ChatGroupFormatter() {
        registerSerializer(ChatGroup.class, new ChatGroupSerializer());
        initObjectMapper();
        filteredFields = new HashSet<>();
        Collections.addAll(filteredFields,
                ChatGroup.FD_GROUPID, ChatGroup.FD_NAME,
                ChatGroup.FD_AVATAR, ChatGroup.FD_DESC, ChatGroup.FD_MAXUSERS

        );
    }

    private class ChatGroupSerializer extends AbstractSerializer<ChatGroup> {
        @Override
        public void serialize(ChatGroup chatGroup, JsonGenerator jgen, SerializerProvider serializerProvider)
                throws IOException {
            jgen.writeStartObject();
            jgen.writeObjectField(ChatGroup.FD_GROUPID, getValue(chatGroup.getGroupId()));
            jgen.writeStringField(ChatGroup.FD_NAME, getString(chatGroup.getName()));
            jgen.writeStringField(ChatGroup.FD_AVATAR, getString(chatGroup.getAvatar()));
            jgen.writeStringField(ChatGroup.FD_DESC, getString(chatGroup.getDesc()));
            jgen.writeObjectField(ChatGroup.FD_MAXUSERS, getValue(chatGroup.getMaxUsers()));
            jgen.writeEndObject();
        }
    }

    protected ObjectMapper initObjectMapper() {
        mapper = new ObjectMapper();
        mapper.registerModule(module);
        return mapper;
    }
}