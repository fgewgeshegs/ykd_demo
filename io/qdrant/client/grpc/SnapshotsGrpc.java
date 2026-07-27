package io.qdrant.client.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.65.1)",
    comments = "Source: snapshots_service.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class SnapshotsGrpc {

  private SnapshotsGrpc() {}

  public static final java.lang.String SERVICE_NAME = "qdrant.Snapshots";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.SnapshotsService.CreateSnapshotRequest,
      io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse> getCreateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Create",
      requestType = io.qdrant.client.grpc.SnapshotsService.CreateSnapshotRequest.class,
      responseType = io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.SnapshotsService.CreateSnapshotRequest,
      io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse> getCreateMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.SnapshotsService.CreateSnapshotRequest, io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse> getCreateMethod;
    if ((getCreateMethod = SnapshotsGrpc.getCreateMethod) == null) {
      synchronized (SnapshotsGrpc.class) {
        if ((getCreateMethod = SnapshotsGrpc.getCreateMethod) == null) {
          SnapshotsGrpc.getCreateMethod = getCreateMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.SnapshotsService.CreateSnapshotRequest, io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Create"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.SnapshotsService.CreateSnapshotRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SnapshotsMethodDescriptorSupplier("Create"))
              .build();
        }
      }
    }
    return getCreateMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.SnapshotsService.ListSnapshotsRequest,
      io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse> getListMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "List",
      requestType = io.qdrant.client.grpc.SnapshotsService.ListSnapshotsRequest.class,
      responseType = io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.SnapshotsService.ListSnapshotsRequest,
      io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse> getListMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.SnapshotsService.ListSnapshotsRequest, io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse> getListMethod;
    if ((getListMethod = SnapshotsGrpc.getListMethod) == null) {
      synchronized (SnapshotsGrpc.class) {
        if ((getListMethod = SnapshotsGrpc.getListMethod) == null) {
          SnapshotsGrpc.getListMethod = getListMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.SnapshotsService.ListSnapshotsRequest, io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "List"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.SnapshotsService.ListSnapshotsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SnapshotsMethodDescriptorSupplier("List"))
              .build();
        }
      }
    }
    return getListMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotRequest,
      io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse> getDeleteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Delete",
      requestType = io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotRequest.class,
      responseType = io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotRequest,
      io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse> getDeleteMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotRequest, io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse> getDeleteMethod;
    if ((getDeleteMethod = SnapshotsGrpc.getDeleteMethod) == null) {
      synchronized (SnapshotsGrpc.class) {
        if ((getDeleteMethod = SnapshotsGrpc.getDeleteMethod) == null) {
          SnapshotsGrpc.getDeleteMethod = getDeleteMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotRequest, io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Delete"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SnapshotsMethodDescriptorSupplier("Delete"))
              .build();
        }
      }
    }
    return getDeleteMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.SnapshotsService.CreateFullSnapshotRequest,
      io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse> getCreateFullMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateFull",
      requestType = io.qdrant.client.grpc.SnapshotsService.CreateFullSnapshotRequest.class,
      responseType = io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.SnapshotsService.CreateFullSnapshotRequest,
      io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse> getCreateFullMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.SnapshotsService.CreateFullSnapshotRequest, io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse> getCreateFullMethod;
    if ((getCreateFullMethod = SnapshotsGrpc.getCreateFullMethod) == null) {
      synchronized (SnapshotsGrpc.class) {
        if ((getCreateFullMethod = SnapshotsGrpc.getCreateFullMethod) == null) {
          SnapshotsGrpc.getCreateFullMethod = getCreateFullMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.SnapshotsService.CreateFullSnapshotRequest, io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateFull"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.SnapshotsService.CreateFullSnapshotRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SnapshotsMethodDescriptorSupplier("CreateFull"))
              .build();
        }
      }
    }
    return getCreateFullMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.SnapshotsService.ListFullSnapshotsRequest,
      io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse> getListFullMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListFull",
      requestType = io.qdrant.client.grpc.SnapshotsService.ListFullSnapshotsRequest.class,
      responseType = io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.SnapshotsService.ListFullSnapshotsRequest,
      io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse> getListFullMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.SnapshotsService.ListFullSnapshotsRequest, io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse> getListFullMethod;
    if ((getListFullMethod = SnapshotsGrpc.getListFullMethod) == null) {
      synchronized (SnapshotsGrpc.class) {
        if ((getListFullMethod = SnapshotsGrpc.getListFullMethod) == null) {
          SnapshotsGrpc.getListFullMethod = getListFullMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.SnapshotsService.ListFullSnapshotsRequest, io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListFull"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.SnapshotsService.ListFullSnapshotsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SnapshotsMethodDescriptorSupplier("ListFull"))
              .build();
        }
      }
    }
    return getListFullMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.SnapshotsService.DeleteFullSnapshotRequest,
      io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse> getDeleteFullMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteFull",
      requestType = io.qdrant.client.grpc.SnapshotsService.DeleteFullSnapshotRequest.class,
      responseType = io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.SnapshotsService.DeleteFullSnapshotRequest,
      io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse> getDeleteFullMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.SnapshotsService.DeleteFullSnapshotRequest, io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse> getDeleteFullMethod;
    if ((getDeleteFullMethod = SnapshotsGrpc.getDeleteFullMethod) == null) {
      synchronized (SnapshotsGrpc.class) {
        if ((getDeleteFullMethod = SnapshotsGrpc.getDeleteFullMethod) == null) {
          SnapshotsGrpc.getDeleteFullMethod = getDeleteFullMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.SnapshotsService.DeleteFullSnapshotRequest, io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteFull"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.SnapshotsService.DeleteFullSnapshotRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SnapshotsMethodDescriptorSupplier("DeleteFull"))
              .build();
        }
      }
    }
    return getDeleteFullMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static SnapshotsStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SnapshotsStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SnapshotsStub>() {
        @java.lang.Override
        public SnapshotsStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SnapshotsStub(channel, callOptions);
        }
      };
    return SnapshotsStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static SnapshotsBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SnapshotsBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SnapshotsBlockingStub>() {
        @java.lang.Override
        public SnapshotsBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SnapshotsBlockingStub(channel, callOptions);
        }
      };
    return SnapshotsBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static SnapshotsFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SnapshotsFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SnapshotsFutureStub>() {
        @java.lang.Override
        public SnapshotsFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SnapshotsFutureStub(channel, callOptions);
        }
      };
    return SnapshotsFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     *Create collection snapshot
     * </pre>
     */
    default void create(io.qdrant.client.grpc.SnapshotsService.CreateSnapshotRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateMethod(), responseObserver);
    }

    /**
     * <pre>
     *List collection snapshots
     * </pre>
     */
    default void list(io.qdrant.client.grpc.SnapshotsService.ListSnapshotsRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListMethod(), responseObserver);
    }

    /**
     * <pre>
     *Delete collection snapshot
     * </pre>
     */
    default void delete(io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteMethod(), responseObserver);
    }

    /**
     * <pre>
     *Create full storage snapshot
     * </pre>
     */
    default void createFull(io.qdrant.client.grpc.SnapshotsService.CreateFullSnapshotRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateFullMethod(), responseObserver);
    }

    /**
     * <pre>
     *List full storage snapshots
     * </pre>
     */
    default void listFull(io.qdrant.client.grpc.SnapshotsService.ListFullSnapshotsRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListFullMethod(), responseObserver);
    }

    /**
     * <pre>
     *Delete full storage snapshot
     * </pre>
     */
    default void deleteFull(io.qdrant.client.grpc.SnapshotsService.DeleteFullSnapshotRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteFullMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service Snapshots.
   */
  public static abstract class SnapshotsImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return SnapshotsGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service Snapshots.
   */
  public static final class SnapshotsStub
      extends io.grpc.stub.AbstractAsyncStub<SnapshotsStub> {
    private SnapshotsStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SnapshotsStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SnapshotsStub(channel, callOptions);
    }

    /**
     * <pre>
     *Create collection snapshot
     * </pre>
     */
    public void create(io.qdrant.client.grpc.SnapshotsService.CreateSnapshotRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *List collection snapshots
     * </pre>
     */
    public void list(io.qdrant.client.grpc.SnapshotsService.ListSnapshotsRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Delete collection snapshot
     * </pre>
     */
    public void delete(io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Create full storage snapshot
     * </pre>
     */
    public void createFull(io.qdrant.client.grpc.SnapshotsService.CreateFullSnapshotRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateFullMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *List full storage snapshots
     * </pre>
     */
    public void listFull(io.qdrant.client.grpc.SnapshotsService.ListFullSnapshotsRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListFullMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Delete full storage snapshot
     * </pre>
     */
    public void deleteFull(io.qdrant.client.grpc.SnapshotsService.DeleteFullSnapshotRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteFullMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service Snapshots.
   */
  public static final class SnapshotsBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<SnapshotsBlockingStub> {
    private SnapshotsBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SnapshotsBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SnapshotsBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     *Create collection snapshot
     * </pre>
     */
    public io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse create(io.qdrant.client.grpc.SnapshotsService.CreateSnapshotRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *List collection snapshots
     * </pre>
     */
    public io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse list(io.qdrant.client.grpc.SnapshotsService.ListSnapshotsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Delete collection snapshot
     * </pre>
     */
    public io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse delete(io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Create full storage snapshot
     * </pre>
     */
    public io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse createFull(io.qdrant.client.grpc.SnapshotsService.CreateFullSnapshotRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateFullMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *List full storage snapshots
     * </pre>
     */
    public io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse listFull(io.qdrant.client.grpc.SnapshotsService.ListFullSnapshotsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListFullMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Delete full storage snapshot
     * </pre>
     */
    public io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse deleteFull(io.qdrant.client.grpc.SnapshotsService.DeleteFullSnapshotRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteFullMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service Snapshots.
   */
  public static final class SnapshotsFutureStub
      extends io.grpc.stub.AbstractFutureStub<SnapshotsFutureStub> {
    private SnapshotsFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SnapshotsFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SnapshotsFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     *Create collection snapshot
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse> create(
        io.qdrant.client.grpc.SnapshotsService.CreateSnapshotRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *List collection snapshots
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse> list(
        io.qdrant.client.grpc.SnapshotsService.ListSnapshotsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Delete collection snapshot
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse> delete(
        io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Create full storage snapshot
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse> createFull(
        io.qdrant.client.grpc.SnapshotsService.CreateFullSnapshotRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateFullMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *List full storage snapshots
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse> listFull(
        io.qdrant.client.grpc.SnapshotsService.ListFullSnapshotsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListFullMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Delete full storage snapshot
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse> deleteFull(
        io.qdrant.client.grpc.SnapshotsService.DeleteFullSnapshotRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteFullMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE = 0;
  private static final int METHODID_LIST = 1;
  private static final int METHODID_DELETE = 2;
  private static final int METHODID_CREATE_FULL = 3;
  private static final int METHODID_LIST_FULL = 4;
  private static final int METHODID_DELETE_FULL = 5;

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
        case METHODID_CREATE:
          serviceImpl.create((io.qdrant.client.grpc.SnapshotsService.CreateSnapshotRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse>) responseObserver);
          break;
        case METHODID_LIST:
          serviceImpl.list((io.qdrant.client.grpc.SnapshotsService.ListSnapshotsRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse>) responseObserver);
          break;
        case METHODID_DELETE:
          serviceImpl.delete((io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse>) responseObserver);
          break;
        case METHODID_CREATE_FULL:
          serviceImpl.createFull((io.qdrant.client.grpc.SnapshotsService.CreateFullSnapshotRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse>) responseObserver);
          break;
        case METHODID_LIST_FULL:
          serviceImpl.listFull((io.qdrant.client.grpc.SnapshotsService.ListFullSnapshotsRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse>) responseObserver);
          break;
        case METHODID_DELETE_FULL:
          serviceImpl.deleteFull((io.qdrant.client.grpc.SnapshotsService.DeleteFullSnapshotRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse>) responseObserver);
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
          getCreateMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.SnapshotsService.CreateSnapshotRequest,
              io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse>(
                service, METHODID_CREATE)))
        .addMethod(
          getListMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.SnapshotsService.ListSnapshotsRequest,
              io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse>(
                service, METHODID_LIST)))
        .addMethod(
          getDeleteMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotRequest,
              io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse>(
                service, METHODID_DELETE)))
        .addMethod(
          getCreateFullMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.SnapshotsService.CreateFullSnapshotRequest,
              io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse>(
                service, METHODID_CREATE_FULL)))
        .addMethod(
          getListFullMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.SnapshotsService.ListFullSnapshotsRequest,
              io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse>(
                service, METHODID_LIST_FULL)))
        .addMethod(
          getDeleteFullMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.SnapshotsService.DeleteFullSnapshotRequest,
              io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse>(
                service, METHODID_DELETE_FULL)))
        .build();
  }

  private static abstract class SnapshotsBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    SnapshotsBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.qdrant.client.grpc.SnapshotsService.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Snapshots");
    }
  }

  private static final class SnapshotsFileDescriptorSupplier
      extends SnapshotsBaseDescriptorSupplier {
    SnapshotsFileDescriptorSupplier() {}
  }

  private static final class SnapshotsMethodDescriptorSupplier
      extends SnapshotsBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    SnapshotsMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (SnapshotsGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new SnapshotsFileDescriptorSupplier())
              .addMethod(getCreateMethod())
              .addMethod(getListMethod())
              .addMethod(getDeleteMethod())
              .addMethod(getCreateFullMethod())
              .addMethod(getListFullMethod())
              .addMethod(getDeleteFullMethod())
              .build();
        }
      }
    }
    return result;
  }
}
