package com.axonivy.solutions.process.analyser.serializer;

import java.io.IOException;
import java.text.DecimalFormat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class LabelValueSerializer extends JsonSerializer<Float> {

  private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

  @Override
  public void serialize(Float value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
      if (value == null) {
          gen.writeNull();
      } else if (value < 0) {
        gen.writeString("");
      } else {
          gen.writeString(DECIMAL_FORMAT.format(value));
      }
  }

}
