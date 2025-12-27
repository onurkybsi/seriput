package io.seriput.server;

import com.google.common.primitives.Bytes;
import io.seriput.common.ObjectMapperProvider;
import io.seriput.common.serialization.response.Response;
import io.seriput.common.serialization.response.ResponseStatus;
import io.seriput.server.fixture.PartialWritePipeByteChannel;
import io.seriput.server.fixture.PipeByteChannel;
import io.seriput.server.serialization.response.ResponseSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.node.ObjectNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Selector;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.seriput.server.fixture.RequestFixtures.*;
import static io.seriput.server.fixture.ResponseFixtures.deserialize;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

final class SeriputConnectionTest {
  private static final SeriputClient client;
  static {
    try {
      client = new SeriputClient(InetAddress.getByName("localhost"), 6070);
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }

  private ByteChannel connection;
  private Selector selector;

  @BeforeEach
  void setUp() throws IOException {
    this.connection = new PipeByteChannel();
    this.selector = Selector.open();
  }

  @AfterEach
  void cleanUp() throws IOException {
    this.connection.close();
    this.selector.close();
  }

  @Nested
  final class Read {
    @ParameterizedTest
    @ValueSource(ints = {0, 5, 7})
    void should_Read_From_Connection_Open(int numOfBytesToReadAtFirst /* Partial reads */) throws IOException {
      // given
      var requestHandler = mock(RequestHandler.class);
      when(requestHandler.handle(any())).thenReturn(ResponseSerializer.ok());
      var underTest = new SeriputConnection(client, 0, connection, requestHandler, selector);

      byte[] requestPart1 = new byte[numOfBytesToReadAtFirst];
      byte[] requestPart2 = new byte[testPutRequestPayload.length - numOfBytesToReadAtFirst];
      System.arraycopy(testPutRequestPayload, 0, requestPart1, 0, requestPart1.length);
      System.arraycopy(testPutRequestPayload, requestPart1.length, requestPart2, 0, requestPart2.length);

      // when
      connection.write(ByteBuffer.wrap(requestPart1));
      underTest.read();
      connection.write(ByteBuffer.wrap(requestPart2));
      underTest.read();

      // then
      await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> {
        var requestCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(requestHandler, times(1)).handle(requestCaptor.capture());
        assertThat(requestCaptor.getValue()).isEqualTo(testPutRequestPayload);
      });
    }

    @Test
    void should_Not_Do_Anything_When_No_DataAvailable_To_Read() throws IOException {
      // given
      var requestHandler = mock(RequestHandler.class);
      when(requestHandler.handle(any())).thenReturn(ResponseSerializer.ok());
      var underTest = new SeriputConnection(client, 0, connection, requestHandler, selector);
      connection.write(ByteBuffer.wrap(testPutRequestPayload));

      // when
      underTest.read(); // Reads the full request
      await().until(underTest::isReadyToWrite); // Request is handled
      underTest.write(); // Writes the full response
      await().until(() -> !underTest.isReadyToWrite()); // Response is written
      underTest.read(); // Nothing to read, nothing to do

      // then
      verify(requestHandler, times(1)).handle(any());
      verifyNoMoreInteractions(requestHandler);
      assertThat(underTest.isReadyToWrite()).isFalse();
    }

    @Test
    void should_Set_State_As_CLOSING_When_Client_Closes_The_Connection() throws IOException {
      // given
      var requestHandler = mock(RequestHandler.class);
      when(requestHandler.handle(any())).thenReturn(ResponseSerializer.ok());
      var underTest = new SeriputConnection(client, 0, connection, requestHandler, selector);
      connection.close();

      // when
      underTest.read();

      // then
      assertThat(underTest.state()).isEqualTo(SeriputConnection.State.CLOSING);
    }

    @ParameterizedTest
    @EnumSource(value = SeriputConnection.State.class, mode = EnumSource.Mode.EXCLUDE, names = { "OPEN" })
    void should_Throw_IllegalStateException_When_ConnectionState_Is_Other_Than_OPEN(SeriputConnection.State state) {
      // given
      var requestHandler = mock(RequestHandler.class);
      when(requestHandler.handle(any())).thenReturn(ResponseSerializer.ok());
      var underTest = new SeriputConnection(client, 0, connection, requestHandler, selector);
      underTest.state(state);

      // when & then
      assertThatThrownBy(underTest::read)
        .isInstanceOfAny(IllegalStateException.class)
        .hasMessage("Unexpected connection state: " + state);
    }
  }

  @Nested
  final class Write {
    @Test
    @SuppressWarnings("unchecked")
    void should_Write_To_Connection_Open() throws IOException {
      // given
      var underTest = new SeriputConnection(client, 0, connection, new RequestHandlerImpl(emptyList()), selector);

      var requests = ByteBuffer.wrap(
        Bytes.concat(testPutRequestPayload, testGetRequestPayload, testDeleteRequestPayload, testGetRequestPayload));
      connection.write(requests); // Client sends the requests
      underTest.read(); // Server reads the requests

      // when
      await().until(() -> {
        underTest.write();
        return !underTest.isReadyToWrite(); // Writing is finished
      });

      // then
      var responsePayloads = ByteBuffer.wrap(new byte[1024]);
      connection.read(responsePayloads);
      var responses = deserialize(new ByteArrayInputStream(responsePayloads.array()), 4, ObjectNode.class);
      // verify PUT response
      var putResponse = (Response<ObjectNode>) responses[0];
      assertThat(putResponse.status()).isEqualTo(ResponseStatus.OK);
      assertThat(putResponse.value()).isEqualTo(null);
      // verify GET response
      var getResponse = (Response<ObjectNode>) responses[1];
      assertThat(getResponse.status()).isEqualTo(ResponseStatus.OK);
      var expectedValue = ObjectMapperProvider.getInstance().convertValue(testValue, ObjectNode.class);
      assertThat(getResponse.value()).isEqualTo(expectedValue);
      // verify DELETE response
      var deleteResponse = (Response<ObjectNode>) responses[2];
      assertThat(deleteResponse.status()).isEqualTo(ResponseStatus.OK);
      assertThat(deleteResponse.value()).isEqualTo(null);
      // verify GET response
      getResponse = (Response<ObjectNode>) responses[3];
      assertThat(getResponse.status()).isEqualTo(ResponseStatus.NOT_FOUND);
      assertThat(getResponse.value()).isEqualTo(null);
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_Handle_Full_Tcp_SendBuffer() throws IOException {
      // given
      var connection = new PartialWritePipeByteChannel(3);
      var requestHandler = mock(RequestHandler.class);
      when(requestHandler.handle(any())).thenReturn(ResponseSerializer.notFound(), ResponseSerializer.notFound());
      var underTest = new SeriputConnection(client, 0, connection, requestHandler, selector);

      var requests = ByteBuffer.wrap(Bytes.concat(testGetRequestPayload, testDeleteRequestPayload));
      while (connection.write(requests) > 0) { // Client writes the full requests
        connection.write(requests);
      }
      underTest.read(); // Server reads the full requests

      // when
      await().atMost(Duration.ofSeconds(1)).until(() -> {
        underTest.write();
        return !underTest.isReadyToWrite(); //
      });

      // then
      var responsePayloads = ByteBuffer.wrap(new byte[1024]);
      connection.read(responsePayloads);
      var responses = deserialize(new ByteArrayInputStream(responsePayloads.array()), 2, ObjectNode.class);
      // verify GET response
      var getResponse = (Response<ObjectNode>) responses[0];
      assertThat(getResponse.status()).isEqualTo(ResponseStatus.NOT_FOUND);
      assertThat(getResponse.value()).isEqualTo(null);
      // verify DELETE response
      var deleteResponse = (Response<ObjectNode>) responses[1];
      assertThat(deleteResponse.status()).isEqualTo(ResponseStatus.NOT_FOUND);
      assertThat(deleteResponse.value()).isEqualTo(null);
    }

    @Test
    void should_Set_State_As_CLOSING_When_Client_Closes_The_Connection() throws IOException {
      // given
      var requestHandler = mock(RequestHandler.class);
      when(requestHandler.handle(any())).thenReturn(ResponseSerializer.notFound());
      var underTest = new SeriputConnection(client, 0, connection, requestHandler, selector);

      var requests = ByteBuffer.wrap(Bytes.concat(testGetRequestPayload, testDeleteRequestPayload));
      connection.write(requests); // Client writes the full requests
      underTest.read(); // Server reads the full requests
      connection.close(); // Client closes the connection

      await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> { // connection.write(response) won't happen until request is handled
        // when
        underTest.write();

        // then
        assertThat(underTest.state()).isEqualTo(SeriputConnection.State.CLOSING);
      });
    }

    @Test
    void should_Disables_WriteInterest_When_NothingAvailable_To_Write() throws NoSuchFieldException, IllegalAccessException {
      // given
      var underTest = new SeriputConnection(client, 0, connection, new RequestHandlerImpl(emptyList()), selector);
      Field isReadToWriteField = SeriputConnection.class.getDeclaredField("isReadyToWrite");
      isReadToWriteField.setAccessible(true);
      AtomicBoolean isReadyToWrite = (AtomicBoolean) isReadToWriteField.get(underTest);
      isReadyToWrite.set(true);

      // when
      underTest.write();

      // then
      assertThat(underTest.isReadyToWrite()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = SeriputConnection.State.class, mode = EnumSource.Mode.EXCLUDE, names = { "OPEN" })
    void should_Throw_IllegalStateException_When_ConnectionState_Is_Other_Than_OPEN(SeriputConnection.State state) {
      // given
      var requestHandler = mock(RequestHandler.class);
      when(requestHandler.handle(any())).thenReturn(ResponseSerializer.ok());
      var underTest = new SeriputConnection(client, 0, connection, requestHandler, selector);
      underTest.state(state);

      // when & then
      assertThatThrownBy(underTest::write)
        .isInstanceOfAny(IllegalStateException.class)
        .hasMessage("Unexpected connection state: " + state);
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void should_Not_Exit_When_RequestHandler_Throws_Exception() throws IOException {
    // given
    var requestHandler = mock(RequestHandler.class);
    when(requestHandler.handle(any())).thenAnswer(invocation -> {
      var request = invocation.getArgument(0, byte[].class);
      if (Arrays.equals(request, testPutRequestPayload)) {
        Thread.sleep(250);
        throw new RuntimeException("Something went wrong!");
      } else {
        return ResponseSerializer.notFound();
      }
    });
    var underTest = new SeriputConnection(client, 0, connection, requestHandler, selector);
    // Client writes the full requests
    connection.write(ByteBuffer.wrap(Bytes.concat(testPutRequestPayload, testGetRequestPayload)));

    await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> { // Await for RequestHandler
      // when
      underTest.read(); // Server reads the full requests and dispatches
      underTest.write();

      // then
      var requestCaptor = ArgumentCaptor.forClass(byte[].class);
      verify(requestHandler, times(2)).handle(requestCaptor.capture());
      assertThat(requestCaptor.getAllValues().getFirst()).isEqualTo(testPutRequestPayload);
      assertThat(requestCaptor.getAllValues().getLast()).isEqualTo(testGetRequestPayload);
      ByteBuffer responsePayloads = ByteBuffer.wrap(new byte[testGetRequestPayload.length]);
      connection.read(responsePayloads);
      var responses = deserialize(new ByteArrayInputStream(responsePayloads.array()), 1, ObjectNode.class);
      var getResponse = (Response<ObjectNode>) responses[0];
      assertThat(getResponse.status()).isEqualTo(ResponseStatus.NOT_FOUND);
      assertThat(getResponse.value()).isNull();
    });
  }
}
