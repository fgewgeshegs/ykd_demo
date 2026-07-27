package io.qdrant.client.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.65.1)",
    comments = "Source: qdrant.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class QdrantGrpc {

  private QdrantGrpc() {}

  public static final java.lang.String SERVICE_NAME = "qdrant.Qdrant";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.QdrantOuterClass.HealthCheckRequest,
      io.qdrant.client.grpc.QdrantOuterClass.HealthCheckReply> getHealthCheckMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "HealthCheck",
      requestType = io.qdrant.client.grpc.QdrantOuterClass.HealthCheckRequest.class,
      responseType = io.qdrant.client.grpc.QdrantOuterClass.HealthCheckReply.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.QdrantOuterClass.HealthCheckRequest,
      io.qdrant.client.grpc.QdrantOuterClass.HealthCheckReply> getHealthCheckMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.QdrantOuterClass.HealthCheckRequest, io.qdrant.client.grpc.QdrantOuterClass.HealthCheckReply> getHealthCheckMethod;
    if ((getHealthCheckMethod = QdrantGrpc.getHealthCheckMethod) == null) {
      synchronized (QdrantGrpc.class) {
        if ((getHealthCheckMethod = QdrantGrpc.getHealthCheckMethod) == null) {
          QdrantGrpc.getHealthCheckMethod = getHealthCheckMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.QdrantOuterClass.HealthCheckRequest, io.qdrant.client.grpc.QdrantOuterClass.HealthCheckReply>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "HealthCheck"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.QdrantOuterClass.HealthCheckRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.QdrantOuterClass.HealthCheckReply.getDefaultInstance()))
              .setSchemaDescriptor(new QdrantMethodDescriptorSupplier("HealthCheck"))
              .build();
        }
      }
    }
    return getHealthCheckMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static QdrantStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QdrantStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QdrantStub>() {
        @java.lang.Override
        public QdrantStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QdrantStub(channel, callOptions);
        }
      };
    return QdrantStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static QdrantBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QdrantBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QdrantBlockingStub>() {
        @java.lang.Override
        public QdrantBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QdrantBlockingStub(channel, callOptions);
        }
      };
    return QdrantBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static QdrantFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QdrantFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QdrantFutureStub>() {
        @java.lang.Override
        public QdrantFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QdrantFutureStub(channel, callOptions);
        }
      };
    return QdrantFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void healthCheck(io.qdrant.client.grpc.QdrantOuterClass.HealthCheckRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.QdrantOuterClass.HealthCheckReply> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getHealthCheckMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service Qdrant.
   */
  public static abstract class QdrantImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return QdrantGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service Qdrant.
   */
  public static final class QdrantStub
      extends io.grpc.stub.AbstractAsyncStub<QdrantStub> {
    private QdrantStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QdrantStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QdrantStub(channel, callOptions);
    }

    /**
     */
    public void healthCheck(io.qdrant.client.grpc.QdrantOuterClass.HealthCheckRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.QdrantOuterClass.HealthCheckReply> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getHealthCheckMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service Qdrant.
   */
  public static final class QdrantBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<QdrantBlockingStub> {
    private QdrantBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QdrantBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QdrantBlockingStub(channel, callOptions);
    }

    /**
     */
    public io.qdrant.client.grpc.QdrantOuterClass.HealthCheckReply healthCheck(io.qdrant.client.grpc.QdrantOuterClass.HealthCheckRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getHealthCheckMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service Qdrant.
   */
  public static final class QdrantFutureStub
      extends io.grpc.stub.AbstractFutureStub<QdrantFutureStub> {
    private QdrantFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QdrantFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QdrantFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.QdrantOuterClass.HealthCheckReply> healthCheck(
        io.qdrant.client.grpc.QdrantOuterClass.HealthCheckRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getHealthCheckMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_HEALTH_CHECK = 0;

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
        case METHODID_HEALTH_CHECK:
          serviceImpl.healthCheck((io.qdrant.client.grpc.QdrantOuterClass.HealthCheckRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.QdrantOuterClass.HealthCheckReply>) responseObserver);
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
          getHealthCheckMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.QdrantOuterClass.HealthCheckRequest,
              io.qdrant.client.grpc.QdrantOuterClass.HealthCheckReply>(
                service, METHODID_HEALTH_CHECK)))
        .build();
  }

  private static abstract class QdrantBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    QdrantBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.qdrant.client.grpc.QdrantOuterClass.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Qdrant");
    }
  }

  private static final class QdrantFileDescriptorSupplier
      extends QdrantBaseDescriptorSupplier {
    QdrantFileDescriptorSupplier() {}
  }

  private static final class QdrantMethodDescriptorSupplier
      extends QdrantBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    QdrantMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (QdrantGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new QdrantFileDescriptorSupplier())
              .addMethod(getHealthCheckMethod())
              .build();
        }
      }
    }
    return result;
  }
}
