package io.qdrant.client.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.65.1)",
    comments = "Source: raft_service.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class RaftGrpc {

  private RaftGrpc() {}

  public static final java.lang.String SERVICE_NAME = "qdrant.Raft";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.RaftService.RaftMessage,
      com.google.protobuf.Empty> getSendMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Send",
      requestType = io.qdrant.client.grpc.RaftService.RaftMessage.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.RaftService.RaftMessage,
      com.google.protobuf.Empty> getSendMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.RaftService.RaftMessage, com.google.protobuf.Empty> getSendMethod;
    if ((getSendMethod = RaftGrpc.getSendMethod) == null) {
      synchronized (RaftGrpc.class) {
        if ((getSendMethod = RaftGrpc.getSendMethod) == null) {
          RaftGrpc.getSendMethod = getSendMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.RaftService.RaftMessage, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Send"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.RaftService.RaftMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RaftMethodDescriptorSupplier("Send"))
              .build();
        }
      }
    }
    return getSendMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.RaftService.PeerId,
      io.qdrant.client.grpc.RaftService.Uri> getWhoIsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "WhoIs",
      requestType = io.qdrant.client.grpc.RaftService.PeerId.class,
      responseType = io.qdrant.client.grpc.RaftService.Uri.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.RaftService.PeerId,
      io.qdrant.client.grpc.RaftService.Uri> getWhoIsMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.RaftService.PeerId, io.qdrant.client.grpc.RaftService.Uri> getWhoIsMethod;
    if ((getWhoIsMethod = RaftGrpc.getWhoIsMethod) == null) {
      synchronized (RaftGrpc.class) {
        if ((getWhoIsMethod = RaftGrpc.getWhoIsMethod) == null) {
          RaftGrpc.getWhoIsMethod = getWhoIsMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.RaftService.PeerId, io.qdrant.client.grpc.RaftService.Uri>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "WhoIs"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.RaftService.PeerId.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.RaftService.Uri.getDefaultInstance()))
              .setSchemaDescriptor(new RaftMethodDescriptorSupplier("WhoIs"))
              .build();
        }
      }
    }
    return getWhoIsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.RaftService.AddPeerToKnownMessage,
      io.qdrant.client.grpc.RaftService.AllPeers> getAddPeerToKnownMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AddPeerToKnown",
      requestType = io.qdrant.client.grpc.RaftService.AddPeerToKnownMessage.class,
      responseType = io.qdrant.client.grpc.RaftService.AllPeers.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.RaftService.AddPeerToKnownMessage,
      io.qdrant.client.grpc.RaftService.AllPeers> getAddPeerToKnownMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.RaftService.AddPeerToKnownMessage, io.qdrant.client.grpc.RaftService.AllPeers> getAddPeerToKnownMethod;
    if ((getAddPeerToKnownMethod = RaftGrpc.getAddPeerToKnownMethod) == null) {
      synchronized (RaftGrpc.class) {
        if ((getAddPeerToKnownMethod = RaftGrpc.getAddPeerToKnownMethod) == null) {
          RaftGrpc.getAddPeerToKnownMethod = getAddPeerToKnownMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.RaftService.AddPeerToKnownMessage, io.qdrant.client.grpc.RaftService.AllPeers>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AddPeerToKnown"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.RaftService.AddPeerToKnownMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.RaftService.AllPeers.getDefaultInstance()))
              .setSchemaDescriptor(new RaftMethodDescriptorSupplier("AddPeerToKnown"))
              .build();
        }
      }
    }
    return getAddPeerToKnownMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.RaftService.PeerId,
      com.google.protobuf.Empty> getAddPeerAsParticipantMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AddPeerAsParticipant",
      requestType = io.qdrant.client.grpc.RaftService.PeerId.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.RaftService.PeerId,
      com.google.protobuf.Empty> getAddPeerAsParticipantMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.RaftService.PeerId, com.google.protobuf.Empty> getAddPeerAsParticipantMethod;
    if ((getAddPeerAsParticipantMethod = RaftGrpc.getAddPeerAsParticipantMethod) == null) {
      synchronized (RaftGrpc.class) {
        if ((getAddPeerAsParticipantMethod = RaftGrpc.getAddPeerAsParticipantMethod) == null) {
          RaftGrpc.getAddPeerAsParticipantMethod = getAddPeerAsParticipantMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.RaftService.PeerId, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AddPeerAsParticipant"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.RaftService.PeerId.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RaftMethodDescriptorSupplier("AddPeerAsParticipant"))
              .build();
        }
      }
    }
    return getAddPeerAsParticipantMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static RaftStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RaftStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RaftStub>() {
        @java.lang.Override
        public RaftStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RaftStub(channel, callOptions);
        }
      };
    return RaftStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static RaftBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RaftBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RaftBlockingStub>() {
        @java.lang.Override
        public RaftBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RaftBlockingStub(channel, callOptions);
        }
      };
    return RaftBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static RaftFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RaftFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RaftFutureStub>() {
        @java.lang.Override
        public RaftFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RaftFutureStub(channel, callOptions);
        }
      };
    return RaftFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     * Send Raft message to another peer
     * </pre>
     */
    default void send(io.qdrant.client.grpc.RaftService.RaftMessage request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSendMethod(), responseObserver);
    }

    /**
     * <pre>
     * Send to bootstrap peer
     * Returns uri by id if bootstrap knows this peer
     * </pre>
     */
    default void whoIs(io.qdrant.client.grpc.RaftService.PeerId request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.RaftService.Uri> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getWhoIsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Send to bootstrap peer
     * Adds peer to the network
     * Returns all peers
     * </pre>
     */
    default void addPeerToKnown(io.qdrant.client.grpc.RaftService.AddPeerToKnownMessage request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.RaftService.AllPeers> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAddPeerToKnownMethod(), responseObserver);
    }

    /**
     * <pre>
     * DEPRECATED
     * Its functionality is now included in `AddPeerToKnown`
     * Send to bootstrap peer
     * Proposes to add this peer as participant of consensus
     * </pre>
     */
    default void addPeerAsParticipant(io.qdrant.client.grpc.RaftService.PeerId request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAddPeerAsParticipantMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service Raft.
   */
  public static abstract class RaftImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return RaftGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service Raft.
   */
  public static final class RaftStub
      extends io.grpc.stub.AbstractAsyncStub<RaftStub> {
    private RaftStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RaftStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RaftStub(channel, callOptions);
    }

    /**
     * <pre>
     * Send Raft message to another peer
     * </pre>
     */
    public void send(io.qdrant.client.grpc.RaftService.RaftMessage request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSendMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Send to bootstrap peer
     * Returns uri by id if bootstrap knows this peer
     * </pre>
     */
    public void whoIs(io.qdrant.client.grpc.RaftService.PeerId request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.RaftService.Uri> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getWhoIsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Send to bootstrap peer
     * Adds peer to the network
     * Returns all peers
     * </pre>
     */
    public void addPeerToKnown(io.qdrant.client.grpc.RaftService.AddPeerToKnownMessage request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.RaftService.AllPeers> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAddPeerToKnownMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * DEPRECATED
     * Its functionality is now included in `AddPeerToKnown`
     * Send to bootstrap peer
     * Proposes to add this peer as participant of consensus
     * </pre>
     */
    public void addPeerAsParticipant(io.qdrant.client.grpc.RaftService.PeerId request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAddPeerAsParticipantMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service Raft.
   */
  public static final class RaftBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<RaftBlockingStub> {
    private RaftBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RaftBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RaftBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Send Raft message to another peer
     * </pre>
     */
    public com.google.protobuf.Empty send(io.qdrant.client.grpc.RaftService.RaftMessage request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSendMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Send to bootstrap peer
     * Returns uri by id if bootstrap knows this peer
     * </pre>
     */
    public io.qdrant.client.grpc.RaftService.Uri whoIs(io.qdrant.client.grpc.RaftService.PeerId request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getWhoIsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Send to bootstrap peer
     * Adds peer to the network
     * Returns all peers
     * </pre>
     */
    public io.qdrant.client.grpc.RaftService.AllPeers addPeerToKnown(io.qdrant.client.grpc.RaftService.AddPeerToKnownMessage request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAddPeerToKnownMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * DEPRECATED
     * Its functionality is now included in `AddPeerToKnown`
     * Send to bootstrap peer
     * Proposes to add this peer as participant of consensus
     * </pre>
     */
    public com.google.protobuf.Empty addPeerAsParticipant(io.qdrant.client.grpc.RaftService.PeerId request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAddPeerAsParticipantMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service Raft.
   */
  public static final class RaftFutureStub
      extends io.grpc.stub.AbstractFutureStub<RaftFutureStub> {
    private RaftFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RaftFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RaftFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Send Raft message to another peer
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> send(
        io.qdrant.client.grpc.RaftService.RaftMessage request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSendMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Send to bootstrap peer
     * Returns uri by id if bootstrap knows this peer
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.RaftService.Uri> whoIs(
        io.qdrant.client.grpc.RaftService.PeerId request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getWhoIsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Send to bootstrap peer
     * Adds peer to the network
     * Returns all peers
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.RaftService.AllPeers> addPeerToKnown(
        io.qdrant.client.grpc.RaftService.AddPeerToKnownMessage request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAddPeerToKnownMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * DEPRECATED
     * Its functionality is now included in `AddPeerToKnown`
     * Send to bootstrap peer
     * Proposes to add this peer as participant of consensus
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> addPeerAsParticipant(
        io.qdrant.client.grpc.RaftService.PeerId request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAddPeerAsParticipantMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SEND = 0;
  private static final int METHODID_WHO_IS = 1;
  private static final int METHODID_ADD_PEER_TO_KNOWN = 2;
  private static final int METHODID_ADD_PEER_AS_PARTICIPANT = 3;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SEND:
          serviceImpl.send((io.qdrant.client.grpc.RaftService.RaftMessage) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_WHO_IS:
          serviceImpl.whoIs((io.qdrant.client.grpc.RaftService.PeerId) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.RaftService.Uri>) responseObserver);
          break;
        case METHODID_ADD_PEER_TO_KNOWN:
          serviceImpl.addPeerToKnown((io.qdrant.client.grpc.RaftService.AddPeerToKnownMessage) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.RaftService.AllPeers>) responseObserver);
          break;
        case METHODID_ADD_PEER_AS_PARTICIPANT:
          serviceImpl.addPeerAsParticipant((io.qdrant.client.grpc.RaftService.PeerId) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getSendMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.RaftService.RaftMessage,
              com.google.protobuf.Empty>(
                service, METHODID_SEND)))
        .addMethod(
          getWhoIsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.RaftService.PeerId,
              io.qdrant.client.grpc.RaftService.Uri>(
                service, METHODID_WHO_IS)))
        .addMethod(
          getAddPeerToKnownMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.RaftService.AddPeerToKnownMessage,
              io.qdrant.client.grpc.RaftService.AllPeers>(
                service, METHODID_ADD_PEER_TO_KNOWN)))
        .addMethod(
          getAddPeerAsParticipantMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.RaftService.PeerId,
              com.google.protobuf.Empty>(
                service, METHODID_ADD_PEER_AS_PARTICIPANT)))
        .build();
  }

  private static abstract class RaftBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    RaftBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.qdrant.client.grpc.RaftService.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Raft");
    }
  }

  private static final class RaftFileDescriptorSupplier
      extends RaftBaseDescriptorSupplier {
    RaftFileDescriptorSupplier() {}
  }

  private static final class RaftMethodDescriptorSupplier
      extends RaftBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    RaftMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (RaftGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new RaftFileDescriptorSupplier())
              .addMethod(getSendMethod())
              .addMethod(getWhoIsMethod())
              .addMethod(getAddPeerToKnownMethod())
              .addMethod(getAddPeerAsParticipantMethod())
              .build();
        }
      }
    }
    return result;
  }
}
