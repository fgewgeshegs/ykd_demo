package io.qdrant.client.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.65.1)",
    comments = "Source: qdrant_internal_service.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class QdrantInternalGrpc {

  private QdrantInternalGrpc() {}

  public static final java.lang.String SERVICE_NAME = "qdrant.QdrantInternal";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.QdrantInternalService.GetConsensusCommitRequest,
      io.qdrant.client.grpc.QdrantInternalService.GetConsensusCommitResponse> getGetConsensusCommitMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetConsensusCommit",
      requestType = io.qdrant.client.grpc.QdrantInternalService.GetConsensusCommitRequest.class,
      responseType = io.qdrant.client.grpc.QdrantInternalService.GetConsensusCommitResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.QdrantInternalService.GetConsensusCommitRequest,
      io.qdrant.client.grpc.QdrantInternalService.GetConsensusCommitResponse> getGetConsensusCommitMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.QdrantInternalService.GetConsensusCommitRequest, io.qdrant.client.grpc.QdrantInternalService.GetConsensusCommitResponse> getGetConsensusCommitMethod;
    if ((getGetConsensusCommitMethod = QdrantInternalGrpc.getGetConsensusCommitMethod) == null) {
      synchronized (QdrantInternalGrpc.class) {
        if ((getGetConsensusCommitMethod = QdrantInternalGrpc.getGetConsensusCommitMethod) == null) {
          QdrantInternalGrpc.getGetConsensusCommitMethod = getGetConsensusCommitMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.QdrantInternalService.GetConsensusCommitRequest, io.qdrant.client.grpc.QdrantInternalService.GetConsensusCommitResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetConsensusCommit"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.QdrantInternalService.GetConsensusCommitRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.QdrantInternalService.GetConsensusCommitResponse.getDefaultInstance()))
              .setSchemaDescriptor(new QdrantInternalMethodDescriptorSupplier("GetConsensusCommit"))
              .build();
        }
      }
    }
    return getGetConsensusCommitMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.QdrantInternalService.WaitOnConsensusCommitRequest,
      io.qdrant.client.grpc.QdrantInternalService.WaitOnConsensusCommitResponse> getWaitOnConsensusCommitMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "WaitOnConsensusCommit",
      requestType = io.qdrant.client.grpc.QdrantInternalService.WaitOnConsensusCommitRequest.class,
      responseType = io.qdrant.client.grpc.QdrantInternalService.WaitOnConsensusCommitResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.QdrantInternalService.WaitOnConsensusCommitRequest,
      io.qdrant.client.grpc.QdrantInternalService.WaitOnConsensusCommitResponse> getWaitOnConsensusCommitMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.QdrantInternalService.WaitOnConsensusCommitRequest, io.qdrant.client.grpc.QdrantInternalService.WaitOnConsensusCommitResponse> getWaitOnConsensusCommitMethod;
    if ((getWaitOnConsensusCommitMethod = QdrantInternalGrpc.getWaitOnConsensusCommitMethod) == null) {
      synchronized (QdrantInternalGrpc.class) {
        if ((getWaitOnConsensusCommitMethod = QdrantInternalGrpc.getWaitOnConsensusCommitMethod) == null) {
          QdrantInternalGrpc.getWaitOnConsensusCommitMethod = getWaitOnConsensusCommitMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.QdrantInternalService.WaitOnConsensusCommitRequest, io.qdrant.client.grpc.QdrantInternalService.WaitOnConsensusCommitResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "WaitOnConsensusCommit"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.QdrantInternalService.WaitOnConsensusCommitRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.QdrantInternalService.WaitOnConsensusCommitResponse.getDefaultInstance()))
              .setSchemaDescriptor(new QdrantInternalMethodDescriptorSupplier("WaitOnConsensusCommit"))
              .build();
        }
      }
    }
    return getWaitOnConsensusCommitMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static QdrantInternalStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QdrantInternalStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QdrantInternalStub>() {
        @java.lang.Override
        public QdrantInternalStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QdrantInternalStub(channel, callOptions);
        }
      };
    return QdrantInternalStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static QdrantInternalBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QdrantInternalBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QdrantInternalBlockingStub>() {
        @java.lang.Override
        public QdrantInternalBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QdrantInternalBlockingStub(channel, callOptions);
        }
      };
    return QdrantInternalBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static QdrantInternalFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QdrantInternalFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QdrantInternalFutureStub>() {
        @java.lang.Override
        public QdrantInternalFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QdrantInternalFutureStub(channel, callOptions);
        }
      };
    return QdrantInternalFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     *Get current commit and term on the target node.
     * </pre>
     */
    default void getConsensusCommit(io.qdrant.client.grpc.QdrantInternalService.GetConsensusCommitRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.QdrantInternalService.GetConsensusCommitResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetConsensusCommitMethod(), responseObserver);
    }

    /**
     * <pre>
     *Wait until the target node reached the given commit ID.
     * </pre>
     */
    default void waitOnConsensusCommit(io.qdrant.client.grpc.QdrantInternalService.WaitOnConsensusCommitRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.QdrantInternalService.WaitOnConsensusCommitResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getWaitOnConsensusCommitMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service QdrantInternal.
   */
  public static abstract class QdrantInternalImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return QdrantInternalGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service QdrantInternal.
   */
  public static final class QdrantInternalStub
      extends io.grpc.stub.AbstractAsyncStub<QdrantInternalStub> {
    private QdrantInternalStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QdrantInternalStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QdrantInternalStub(channel, callOptions);
    }

    /**
     * <pre>
     *Get current commit and term on the target node.
     * </pre>
     */
    public void getConsensusCommit(io.qdrant.client.grpc.QdrantInternalService.GetConsensusCommitRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.QdrantInternalService.GetConsensusCommitResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetConsensusCommitMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Wait until the target node reached the given commit ID.
     * </pre>
     */
    public void waitOnConsensusCommit(io.qdrant.client.grpc.QdrantInternalService.WaitOnConsensusCommitRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.QdrantInternalService.WaitOnConsensusCommitResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getWaitOnConsensusCommitMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service QdrantInternal.
   */
  public static final class QdrantInternalBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<QdrantInternalBlockingStub> {
    private QdrantInternalBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QdrantInternalBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QdrantInternalBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     *Get current commit and term on the target node.
     * </pre>
     */
    public io.qdrant.client.grpc.QdrantInternalService.GetConsensusCommitResponse getConsensusCommit(io.qdrant.client.grpc.QdrantInternalService.GetConsensusCommitRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetConsensusCommitMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Wait until the target node reached the given commit ID.
     * </pre>
     */
    public io.qdrant.client.grpc.QdrantInternalService.WaitOnConsensusCommitResponse waitOnConsensusCommit(io.qdrant.client.grpc.QdrantInternalService.WaitOnConsensusCommitRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getWaitOnConsensusCommitMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service QdrantInternal.
   */
  public static final class QdrantInternalFutureStub
      extends io.grpc.stub.AbstractFutureStub<QdrantInternalFutureStub> {
    private QdrantInternalFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QdrantInternalFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QdrantInternalFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     *Get current commit and term on the target node.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.QdrantInternalService.GetConsensusCommitResponse> getConsensusCommit(
        io.qdrant.client.grpc.QdrantInternalService.GetConsensusCommitRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetConsensusCommitMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Wait until the target node reached the given commit ID.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.QdrantInternalService.WaitOnConsensusCommitResponse> waitOnConsensusCommit(
        io.qdrant.client.grpc.QdrantInternalService.WaitOnConsensusCommitRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getWaitOnConsensusCommitMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_CONSENSUS_COMMIT = 0;
  private static final int METHODID_WAIT_ON_CONSENSUS_COMMIT = 1;

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
        case METHODID_GET_CONSENSUS_COMMIT:
          serviceImpl.getConsensusCommit((io.qdrant.client.grpc.QdrantInternalService.GetConsensusCommitRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.QdrantInternalService.GetConsensusCommitResponse>) responseObserver);
          break;
        case METHODID_WAIT_ON_CONSENSUS_COMMIT:
          serviceImpl.waitOnConsensusCommit((io.qdrant.client.grpc.QdrantInternalService.WaitOnConsensusCommitRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.QdrantInternalService.WaitOnConsensusCommitResponse>) responseObserver);
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
          getGetConsensusCommitMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.QdrantInternalService.GetConsensusCommitRequest,
              io.qdrant.client.grpc.QdrantInternalService.GetConsensusCommitResponse>(
                service, METHODID_GET_CONSENSUS_COMMIT)))
        .addMethod(
          getWaitOnConsensusCommitMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.QdrantInternalService.WaitOnConsensusCommitRequest,
              io.qdrant.client.grpc.QdrantInternalService.WaitOnConsensusCommitResponse>(
                service, METHODID_WAIT_ON_CONSENSUS_COMMIT)))
        .build();
  }

  private static abstract class QdrantInternalBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    QdrantInternalBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.qdrant.client.grpc.QdrantInternalService.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("QdrantInternal");
    }
  }

  private static final class QdrantInternalFileDescriptorSupplier
      extends QdrantInternalBaseDescriptorSupplier {
    QdrantInternalFileDescriptorSupplier() {}
  }

  private static final class QdrantInternalMethodDescriptorSupplier
      extends QdrantInternalBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    QdrantInternalMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (QdrantInternalGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new QdrantInternalFileDescriptorSupplier())
              .addMethod(getGetConsensusCommitMethod())
              .addMethod(getWaitOnConsensusCommitMethod())
              .build();
        }
      }
    }
    return result;
  }
}
