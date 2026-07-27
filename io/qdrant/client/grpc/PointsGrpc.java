package io.qdrant.client.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.65.1)",
    comments = "Source: points_service.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class PointsGrpc {

  private PointsGrpc() {}

  public static final java.lang.String SERVICE_NAME = "qdrant.Points";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.UpsertPoints,
      io.qdrant.client.grpc.Points.PointsOperationResponse> getUpsertMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Upsert",
      requestType = io.qdrant.client.grpc.Points.UpsertPoints.class,
      responseType = io.qdrant.client.grpc.Points.PointsOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.UpsertPoints,
      io.qdrant.client.grpc.Points.PointsOperationResponse> getUpsertMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.UpsertPoints, io.qdrant.client.grpc.Points.PointsOperationResponse> getUpsertMethod;
    if ((getUpsertMethod = PointsGrpc.getUpsertMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getUpsertMethod = PointsGrpc.getUpsertMethod) == null) {
          PointsGrpc.getUpsertMethod = getUpsertMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.UpsertPoints, io.qdrant.client.grpc.Points.PointsOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Upsert"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.UpsertPoints.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.PointsOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("Upsert"))
              .build();
        }
      }
    }
    return getUpsertMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.DeletePoints,
      io.qdrant.client.grpc.Points.PointsOperationResponse> getDeleteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Delete",
      requestType = io.qdrant.client.grpc.Points.DeletePoints.class,
      responseType = io.qdrant.client.grpc.Points.PointsOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.DeletePoints,
      io.qdrant.client.grpc.Points.PointsOperationResponse> getDeleteMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.DeletePoints, io.qdrant.client.grpc.Points.PointsOperationResponse> getDeleteMethod;
    if ((getDeleteMethod = PointsGrpc.getDeleteMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getDeleteMethod = PointsGrpc.getDeleteMethod) == null) {
          PointsGrpc.getDeleteMethod = getDeleteMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.DeletePoints, io.qdrant.client.grpc.Points.PointsOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Delete"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.DeletePoints.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.PointsOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("Delete"))
              .build();
        }
      }
    }
    return getDeleteMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.GetPoints,
      io.qdrant.client.grpc.Points.GetResponse> getGetMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Get",
      requestType = io.qdrant.client.grpc.Points.GetPoints.class,
      responseType = io.qdrant.client.grpc.Points.GetResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.GetPoints,
      io.qdrant.client.grpc.Points.GetResponse> getGetMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.GetPoints, io.qdrant.client.grpc.Points.GetResponse> getGetMethod;
    if ((getGetMethod = PointsGrpc.getGetMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getGetMethod = PointsGrpc.getGetMethod) == null) {
          PointsGrpc.getGetMethod = getGetMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.GetPoints, io.qdrant.client.grpc.Points.GetResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Get"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.GetPoints.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.GetResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("Get"))
              .build();
        }
      }
    }
    return getGetMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.UpdatePointVectors,
      io.qdrant.client.grpc.Points.PointsOperationResponse> getUpdateVectorsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateVectors",
      requestType = io.qdrant.client.grpc.Points.UpdatePointVectors.class,
      responseType = io.qdrant.client.grpc.Points.PointsOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.UpdatePointVectors,
      io.qdrant.client.grpc.Points.PointsOperationResponse> getUpdateVectorsMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.UpdatePointVectors, io.qdrant.client.grpc.Points.PointsOperationResponse> getUpdateVectorsMethod;
    if ((getUpdateVectorsMethod = PointsGrpc.getUpdateVectorsMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getUpdateVectorsMethod = PointsGrpc.getUpdateVectorsMethod) == null) {
          PointsGrpc.getUpdateVectorsMethod = getUpdateVectorsMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.UpdatePointVectors, io.qdrant.client.grpc.Points.PointsOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateVectors"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.UpdatePointVectors.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.PointsOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("UpdateVectors"))
              .build();
        }
      }
    }
    return getUpdateVectorsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.DeletePointVectors,
      io.qdrant.client.grpc.Points.PointsOperationResponse> getDeleteVectorsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteVectors",
      requestType = io.qdrant.client.grpc.Points.DeletePointVectors.class,
      responseType = io.qdrant.client.grpc.Points.PointsOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.DeletePointVectors,
      io.qdrant.client.grpc.Points.PointsOperationResponse> getDeleteVectorsMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.DeletePointVectors, io.qdrant.client.grpc.Points.PointsOperationResponse> getDeleteVectorsMethod;
    if ((getDeleteVectorsMethod = PointsGrpc.getDeleteVectorsMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getDeleteVectorsMethod = PointsGrpc.getDeleteVectorsMethod) == null) {
          PointsGrpc.getDeleteVectorsMethod = getDeleteVectorsMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.DeletePointVectors, io.qdrant.client.grpc.Points.PointsOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteVectors"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.DeletePointVectors.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.PointsOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("DeleteVectors"))
              .build();
        }
      }
    }
    return getDeleteVectorsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.SetPayloadPoints,
      io.qdrant.client.grpc.Points.PointsOperationResponse> getSetPayloadMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetPayload",
      requestType = io.qdrant.client.grpc.Points.SetPayloadPoints.class,
      responseType = io.qdrant.client.grpc.Points.PointsOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.SetPayloadPoints,
      io.qdrant.client.grpc.Points.PointsOperationResponse> getSetPayloadMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.SetPayloadPoints, io.qdrant.client.grpc.Points.PointsOperationResponse> getSetPayloadMethod;
    if ((getSetPayloadMethod = PointsGrpc.getSetPayloadMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getSetPayloadMethod = PointsGrpc.getSetPayloadMethod) == null) {
          PointsGrpc.getSetPayloadMethod = getSetPayloadMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.SetPayloadPoints, io.qdrant.client.grpc.Points.PointsOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetPayload"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.SetPayloadPoints.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.PointsOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("SetPayload"))
              .build();
        }
      }
    }
    return getSetPayloadMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.SetPayloadPoints,
      io.qdrant.client.grpc.Points.PointsOperationResponse> getOverwritePayloadMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "OverwritePayload",
      requestType = io.qdrant.client.grpc.Points.SetPayloadPoints.class,
      responseType = io.qdrant.client.grpc.Points.PointsOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.SetPayloadPoints,
      io.qdrant.client.grpc.Points.PointsOperationResponse> getOverwritePayloadMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.SetPayloadPoints, io.qdrant.client.grpc.Points.PointsOperationResponse> getOverwritePayloadMethod;
    if ((getOverwritePayloadMethod = PointsGrpc.getOverwritePayloadMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getOverwritePayloadMethod = PointsGrpc.getOverwritePayloadMethod) == null) {
          PointsGrpc.getOverwritePayloadMethod = getOverwritePayloadMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.SetPayloadPoints, io.qdrant.client.grpc.Points.PointsOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "OverwritePayload"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.SetPayloadPoints.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.PointsOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("OverwritePayload"))
              .build();
        }
      }
    }
    return getOverwritePayloadMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.DeletePayloadPoints,
      io.qdrant.client.grpc.Points.PointsOperationResponse> getDeletePayloadMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeletePayload",
      requestType = io.qdrant.client.grpc.Points.DeletePayloadPoints.class,
      responseType = io.qdrant.client.grpc.Points.PointsOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.DeletePayloadPoints,
      io.qdrant.client.grpc.Points.PointsOperationResponse> getDeletePayloadMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.DeletePayloadPoints, io.qdrant.client.grpc.Points.PointsOperationResponse> getDeletePayloadMethod;
    if ((getDeletePayloadMethod = PointsGrpc.getDeletePayloadMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getDeletePayloadMethod = PointsGrpc.getDeletePayloadMethod) == null) {
          PointsGrpc.getDeletePayloadMethod = getDeletePayloadMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.DeletePayloadPoints, io.qdrant.client.grpc.Points.PointsOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeletePayload"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.DeletePayloadPoints.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.PointsOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("DeletePayload"))
              .build();
        }
      }
    }
    return getDeletePayloadMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.ClearPayloadPoints,
      io.qdrant.client.grpc.Points.PointsOperationResponse> getClearPayloadMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ClearPayload",
      requestType = io.qdrant.client.grpc.Points.ClearPayloadPoints.class,
      responseType = io.qdrant.client.grpc.Points.PointsOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.ClearPayloadPoints,
      io.qdrant.client.grpc.Points.PointsOperationResponse> getClearPayloadMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.ClearPayloadPoints, io.qdrant.client.grpc.Points.PointsOperationResponse> getClearPayloadMethod;
    if ((getClearPayloadMethod = PointsGrpc.getClearPayloadMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getClearPayloadMethod = PointsGrpc.getClearPayloadMethod) == null) {
          PointsGrpc.getClearPayloadMethod = getClearPayloadMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.ClearPayloadPoints, io.qdrant.client.grpc.Points.PointsOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ClearPayload"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.ClearPayloadPoints.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.PointsOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("ClearPayload"))
              .build();
        }
      }
    }
    return getClearPayloadMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.CreateFieldIndexCollection,
      io.qdrant.client.grpc.Points.PointsOperationResponse> getCreateFieldIndexMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateFieldIndex",
      requestType = io.qdrant.client.grpc.Points.CreateFieldIndexCollection.class,
      responseType = io.qdrant.client.grpc.Points.PointsOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.CreateFieldIndexCollection,
      io.qdrant.client.grpc.Points.PointsOperationResponse> getCreateFieldIndexMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.CreateFieldIndexCollection, io.qdrant.client.grpc.Points.PointsOperationResponse> getCreateFieldIndexMethod;
    if ((getCreateFieldIndexMethod = PointsGrpc.getCreateFieldIndexMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getCreateFieldIndexMethod = PointsGrpc.getCreateFieldIndexMethod) == null) {
          PointsGrpc.getCreateFieldIndexMethod = getCreateFieldIndexMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.CreateFieldIndexCollection, io.qdrant.client.grpc.Points.PointsOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateFieldIndex"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.CreateFieldIndexCollection.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.PointsOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("CreateFieldIndex"))
              .build();
        }
      }
    }
    return getCreateFieldIndexMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.DeleteFieldIndexCollection,
      io.qdrant.client.grpc.Points.PointsOperationResponse> getDeleteFieldIndexMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteFieldIndex",
      requestType = io.qdrant.client.grpc.Points.DeleteFieldIndexCollection.class,
      responseType = io.qdrant.client.grpc.Points.PointsOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.DeleteFieldIndexCollection,
      io.qdrant.client.grpc.Points.PointsOperationResponse> getDeleteFieldIndexMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.DeleteFieldIndexCollection, io.qdrant.client.grpc.Points.PointsOperationResponse> getDeleteFieldIndexMethod;
    if ((getDeleteFieldIndexMethod = PointsGrpc.getDeleteFieldIndexMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getDeleteFieldIndexMethod = PointsGrpc.getDeleteFieldIndexMethod) == null) {
          PointsGrpc.getDeleteFieldIndexMethod = getDeleteFieldIndexMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.DeleteFieldIndexCollection, io.qdrant.client.grpc.Points.PointsOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteFieldIndex"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.DeleteFieldIndexCollection.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.PointsOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("DeleteFieldIndex"))
              .build();
        }
      }
    }
    return getDeleteFieldIndexMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.SearchPoints,
      io.qdrant.client.grpc.Points.SearchResponse> getSearchMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Search",
      requestType = io.qdrant.client.grpc.Points.SearchPoints.class,
      responseType = io.qdrant.client.grpc.Points.SearchResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.SearchPoints,
      io.qdrant.client.grpc.Points.SearchResponse> getSearchMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.SearchPoints, io.qdrant.client.grpc.Points.SearchResponse> getSearchMethod;
    if ((getSearchMethod = PointsGrpc.getSearchMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getSearchMethod = PointsGrpc.getSearchMethod) == null) {
          PointsGrpc.getSearchMethod = getSearchMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.SearchPoints, io.qdrant.client.grpc.Points.SearchResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Search"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.SearchPoints.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.SearchResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("Search"))
              .build();
        }
      }
    }
    return getSearchMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.SearchBatchPoints,
      io.qdrant.client.grpc.Points.SearchBatchResponse> getSearchBatchMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SearchBatch",
      requestType = io.qdrant.client.grpc.Points.SearchBatchPoints.class,
      responseType = io.qdrant.client.grpc.Points.SearchBatchResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.SearchBatchPoints,
      io.qdrant.client.grpc.Points.SearchBatchResponse> getSearchBatchMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.SearchBatchPoints, io.qdrant.client.grpc.Points.SearchBatchResponse> getSearchBatchMethod;
    if ((getSearchBatchMethod = PointsGrpc.getSearchBatchMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getSearchBatchMethod = PointsGrpc.getSearchBatchMethod) == null) {
          PointsGrpc.getSearchBatchMethod = getSearchBatchMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.SearchBatchPoints, io.qdrant.client.grpc.Points.SearchBatchResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SearchBatch"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.SearchBatchPoints.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.SearchBatchResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("SearchBatch"))
              .build();
        }
      }
    }
    return getSearchBatchMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.SearchPointGroups,
      io.qdrant.client.grpc.Points.SearchGroupsResponse> getSearchGroupsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SearchGroups",
      requestType = io.qdrant.client.grpc.Points.SearchPointGroups.class,
      responseType = io.qdrant.client.grpc.Points.SearchGroupsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.SearchPointGroups,
      io.qdrant.client.grpc.Points.SearchGroupsResponse> getSearchGroupsMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.SearchPointGroups, io.qdrant.client.grpc.Points.SearchGroupsResponse> getSearchGroupsMethod;
    if ((getSearchGroupsMethod = PointsGrpc.getSearchGroupsMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getSearchGroupsMethod = PointsGrpc.getSearchGroupsMethod) == null) {
          PointsGrpc.getSearchGroupsMethod = getSearchGroupsMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.SearchPointGroups, io.qdrant.client.grpc.Points.SearchGroupsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SearchGroups"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.SearchPointGroups.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.SearchGroupsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("SearchGroups"))
              .build();
        }
      }
    }
    return getSearchGroupsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.ScrollPoints,
      io.qdrant.client.grpc.Points.ScrollResponse> getScrollMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Scroll",
      requestType = io.qdrant.client.grpc.Points.ScrollPoints.class,
      responseType = io.qdrant.client.grpc.Points.ScrollResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.ScrollPoints,
      io.qdrant.client.grpc.Points.ScrollResponse> getScrollMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.ScrollPoints, io.qdrant.client.grpc.Points.ScrollResponse> getScrollMethod;
    if ((getScrollMethod = PointsGrpc.getScrollMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getScrollMethod = PointsGrpc.getScrollMethod) == null) {
          PointsGrpc.getScrollMethod = getScrollMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.ScrollPoints, io.qdrant.client.grpc.Points.ScrollResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Scroll"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.ScrollPoints.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.ScrollResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("Scroll"))
              .build();
        }
      }
    }
    return getScrollMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.RecommendPoints,
      io.qdrant.client.grpc.Points.RecommendResponse> getRecommendMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Recommend",
      requestType = io.qdrant.client.grpc.Points.RecommendPoints.class,
      responseType = io.qdrant.client.grpc.Points.RecommendResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.RecommendPoints,
      io.qdrant.client.grpc.Points.RecommendResponse> getRecommendMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.RecommendPoints, io.qdrant.client.grpc.Points.RecommendResponse> getRecommendMethod;
    if ((getRecommendMethod = PointsGrpc.getRecommendMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getRecommendMethod = PointsGrpc.getRecommendMethod) == null) {
          PointsGrpc.getRecommendMethod = getRecommendMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.RecommendPoints, io.qdrant.client.grpc.Points.RecommendResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Recommend"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.RecommendPoints.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.RecommendResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("Recommend"))
              .build();
        }
      }
    }
    return getRecommendMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.RecommendBatchPoints,
      io.qdrant.client.grpc.Points.RecommendBatchResponse> getRecommendBatchMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RecommendBatch",
      requestType = io.qdrant.client.grpc.Points.RecommendBatchPoints.class,
      responseType = io.qdrant.client.grpc.Points.RecommendBatchResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.RecommendBatchPoints,
      io.qdrant.client.grpc.Points.RecommendBatchResponse> getRecommendBatchMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.RecommendBatchPoints, io.qdrant.client.grpc.Points.RecommendBatchResponse> getRecommendBatchMethod;
    if ((getRecommendBatchMethod = PointsGrpc.getRecommendBatchMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getRecommendBatchMethod = PointsGrpc.getRecommendBatchMethod) == null) {
          PointsGrpc.getRecommendBatchMethod = getRecommendBatchMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.RecommendBatchPoints, io.qdrant.client.grpc.Points.RecommendBatchResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RecommendBatch"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.RecommendBatchPoints.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.RecommendBatchResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("RecommendBatch"))
              .build();
        }
      }
    }
    return getRecommendBatchMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.RecommendPointGroups,
      io.qdrant.client.grpc.Points.RecommendGroupsResponse> getRecommendGroupsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RecommendGroups",
      requestType = io.qdrant.client.grpc.Points.RecommendPointGroups.class,
      responseType = io.qdrant.client.grpc.Points.RecommendGroupsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.RecommendPointGroups,
      io.qdrant.client.grpc.Points.RecommendGroupsResponse> getRecommendGroupsMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.RecommendPointGroups, io.qdrant.client.grpc.Points.RecommendGroupsResponse> getRecommendGroupsMethod;
    if ((getRecommendGroupsMethod = PointsGrpc.getRecommendGroupsMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getRecommendGroupsMethod = PointsGrpc.getRecommendGroupsMethod) == null) {
          PointsGrpc.getRecommendGroupsMethod = getRecommendGroupsMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.RecommendPointGroups, io.qdrant.client.grpc.Points.RecommendGroupsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RecommendGroups"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.RecommendPointGroups.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.RecommendGroupsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("RecommendGroups"))
              .build();
        }
      }
    }
    return getRecommendGroupsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.DiscoverPoints,
      io.qdrant.client.grpc.Points.DiscoverResponse> getDiscoverMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Discover",
      requestType = io.qdrant.client.grpc.Points.DiscoverPoints.class,
      responseType = io.qdrant.client.grpc.Points.DiscoverResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.DiscoverPoints,
      io.qdrant.client.grpc.Points.DiscoverResponse> getDiscoverMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.DiscoverPoints, io.qdrant.client.grpc.Points.DiscoverResponse> getDiscoverMethod;
    if ((getDiscoverMethod = PointsGrpc.getDiscoverMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getDiscoverMethod = PointsGrpc.getDiscoverMethod) == null) {
          PointsGrpc.getDiscoverMethod = getDiscoverMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.DiscoverPoints, io.qdrant.client.grpc.Points.DiscoverResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Discover"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.DiscoverPoints.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.DiscoverResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("Discover"))
              .build();
        }
      }
    }
    return getDiscoverMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.DiscoverBatchPoints,
      io.qdrant.client.grpc.Points.DiscoverBatchResponse> getDiscoverBatchMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DiscoverBatch",
      requestType = io.qdrant.client.grpc.Points.DiscoverBatchPoints.class,
      responseType = io.qdrant.client.grpc.Points.DiscoverBatchResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.DiscoverBatchPoints,
      io.qdrant.client.grpc.Points.DiscoverBatchResponse> getDiscoverBatchMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.DiscoverBatchPoints, io.qdrant.client.grpc.Points.DiscoverBatchResponse> getDiscoverBatchMethod;
    if ((getDiscoverBatchMethod = PointsGrpc.getDiscoverBatchMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getDiscoverBatchMethod = PointsGrpc.getDiscoverBatchMethod) == null) {
          PointsGrpc.getDiscoverBatchMethod = getDiscoverBatchMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.DiscoverBatchPoints, io.qdrant.client.grpc.Points.DiscoverBatchResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DiscoverBatch"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.DiscoverBatchPoints.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.DiscoverBatchResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("DiscoverBatch"))
              .build();
        }
      }
    }
    return getDiscoverBatchMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.CountPoints,
      io.qdrant.client.grpc.Points.CountResponse> getCountMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Count",
      requestType = io.qdrant.client.grpc.Points.CountPoints.class,
      responseType = io.qdrant.client.grpc.Points.CountResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.CountPoints,
      io.qdrant.client.grpc.Points.CountResponse> getCountMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.CountPoints, io.qdrant.client.grpc.Points.CountResponse> getCountMethod;
    if ((getCountMethod = PointsGrpc.getCountMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getCountMethod = PointsGrpc.getCountMethod) == null) {
          PointsGrpc.getCountMethod = getCountMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.CountPoints, io.qdrant.client.grpc.Points.CountResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Count"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.CountPoints.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.CountResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("Count"))
              .build();
        }
      }
    }
    return getCountMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.UpdateBatchPoints,
      io.qdrant.client.grpc.Points.UpdateBatchResponse> getUpdateBatchMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateBatch",
      requestType = io.qdrant.client.grpc.Points.UpdateBatchPoints.class,
      responseType = io.qdrant.client.grpc.Points.UpdateBatchResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.UpdateBatchPoints,
      io.qdrant.client.grpc.Points.UpdateBatchResponse> getUpdateBatchMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.UpdateBatchPoints, io.qdrant.client.grpc.Points.UpdateBatchResponse> getUpdateBatchMethod;
    if ((getUpdateBatchMethod = PointsGrpc.getUpdateBatchMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getUpdateBatchMethod = PointsGrpc.getUpdateBatchMethod) == null) {
          PointsGrpc.getUpdateBatchMethod = getUpdateBatchMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.UpdateBatchPoints, io.qdrant.client.grpc.Points.UpdateBatchResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateBatch"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.UpdateBatchPoints.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.UpdateBatchResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("UpdateBatch"))
              .build();
        }
      }
    }
    return getUpdateBatchMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.QueryPoints,
      io.qdrant.client.grpc.Points.QueryResponse> getQueryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Query",
      requestType = io.qdrant.client.grpc.Points.QueryPoints.class,
      responseType = io.qdrant.client.grpc.Points.QueryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.QueryPoints,
      io.qdrant.client.grpc.Points.QueryResponse> getQueryMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.QueryPoints, io.qdrant.client.grpc.Points.QueryResponse> getQueryMethod;
    if ((getQueryMethod = PointsGrpc.getQueryMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getQueryMethod = PointsGrpc.getQueryMethod) == null) {
          PointsGrpc.getQueryMethod = getQueryMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.QueryPoints, io.qdrant.client.grpc.Points.QueryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Query"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.QueryPoints.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.QueryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("Query"))
              .build();
        }
      }
    }
    return getQueryMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.QueryBatchPoints,
      io.qdrant.client.grpc.Points.QueryBatchResponse> getQueryBatchMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "QueryBatch",
      requestType = io.qdrant.client.grpc.Points.QueryBatchPoints.class,
      responseType = io.qdrant.client.grpc.Points.QueryBatchResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.QueryBatchPoints,
      io.qdrant.client.grpc.Points.QueryBatchResponse> getQueryBatchMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.QueryBatchPoints, io.qdrant.client.grpc.Points.QueryBatchResponse> getQueryBatchMethod;
    if ((getQueryBatchMethod = PointsGrpc.getQueryBatchMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getQueryBatchMethod = PointsGrpc.getQueryBatchMethod) == null) {
          PointsGrpc.getQueryBatchMethod = getQueryBatchMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.QueryBatchPoints, io.qdrant.client.grpc.Points.QueryBatchResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "QueryBatch"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.QueryBatchPoints.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.QueryBatchResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("QueryBatch"))
              .build();
        }
      }
    }
    return getQueryBatchMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.QueryPointGroups,
      io.qdrant.client.grpc.Points.QueryGroupsResponse> getQueryGroupsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "QueryGroups",
      requestType = io.qdrant.client.grpc.Points.QueryPointGroups.class,
      responseType = io.qdrant.client.grpc.Points.QueryGroupsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.QueryPointGroups,
      io.qdrant.client.grpc.Points.QueryGroupsResponse> getQueryGroupsMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.QueryPointGroups, io.qdrant.client.grpc.Points.QueryGroupsResponse> getQueryGroupsMethod;
    if ((getQueryGroupsMethod = PointsGrpc.getQueryGroupsMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getQueryGroupsMethod = PointsGrpc.getQueryGroupsMethod) == null) {
          PointsGrpc.getQueryGroupsMethod = getQueryGroupsMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.QueryPointGroups, io.qdrant.client.grpc.Points.QueryGroupsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "QueryGroups"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.QueryPointGroups.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.QueryGroupsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("QueryGroups"))
              .build();
        }
      }
    }
    return getQueryGroupsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.FacetCounts,
      io.qdrant.client.grpc.Points.FacetResponse> getFacetMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Facet",
      requestType = io.qdrant.client.grpc.Points.FacetCounts.class,
      responseType = io.qdrant.client.grpc.Points.FacetResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.FacetCounts,
      io.qdrant.client.grpc.Points.FacetResponse> getFacetMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.FacetCounts, io.qdrant.client.grpc.Points.FacetResponse> getFacetMethod;
    if ((getFacetMethod = PointsGrpc.getFacetMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getFacetMethod = PointsGrpc.getFacetMethod) == null) {
          PointsGrpc.getFacetMethod = getFacetMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.FacetCounts, io.qdrant.client.grpc.Points.FacetResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Facet"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.FacetCounts.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.FacetResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("Facet"))
              .build();
        }
      }
    }
    return getFacetMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.SearchMatrixPoints,
      io.qdrant.client.grpc.Points.SearchMatrixPairsResponse> getSearchMatrixPairsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SearchMatrixPairs",
      requestType = io.qdrant.client.grpc.Points.SearchMatrixPoints.class,
      responseType = io.qdrant.client.grpc.Points.SearchMatrixPairsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.SearchMatrixPoints,
      io.qdrant.client.grpc.Points.SearchMatrixPairsResponse> getSearchMatrixPairsMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.SearchMatrixPoints, io.qdrant.client.grpc.Points.SearchMatrixPairsResponse> getSearchMatrixPairsMethod;
    if ((getSearchMatrixPairsMethod = PointsGrpc.getSearchMatrixPairsMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getSearchMatrixPairsMethod = PointsGrpc.getSearchMatrixPairsMethod) == null) {
          PointsGrpc.getSearchMatrixPairsMethod = getSearchMatrixPairsMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.SearchMatrixPoints, io.qdrant.client.grpc.Points.SearchMatrixPairsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SearchMatrixPairs"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.SearchMatrixPoints.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.SearchMatrixPairsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("SearchMatrixPairs"))
              .build();
        }
      }
    }
    return getSearchMatrixPairsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.SearchMatrixPoints,
      io.qdrant.client.grpc.Points.SearchMatrixOffsetsResponse> getSearchMatrixOffsetsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SearchMatrixOffsets",
      requestType = io.qdrant.client.grpc.Points.SearchMatrixPoints.class,
      responseType = io.qdrant.client.grpc.Points.SearchMatrixOffsetsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.SearchMatrixPoints,
      io.qdrant.client.grpc.Points.SearchMatrixOffsetsResponse> getSearchMatrixOffsetsMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Points.SearchMatrixPoints, io.qdrant.client.grpc.Points.SearchMatrixOffsetsResponse> getSearchMatrixOffsetsMethod;
    if ((getSearchMatrixOffsetsMethod = PointsGrpc.getSearchMatrixOffsetsMethod) == null) {
      synchronized (PointsGrpc.class) {
        if ((getSearchMatrixOffsetsMethod = PointsGrpc.getSearchMatrixOffsetsMethod) == null) {
          PointsGrpc.getSearchMatrixOffsetsMethod = getSearchMatrixOffsetsMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Points.SearchMatrixPoints, io.qdrant.client.grpc.Points.SearchMatrixOffsetsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SearchMatrixOffsets"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.SearchMatrixPoints.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.SearchMatrixOffsetsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsMethodDescriptorSupplier("SearchMatrixOffsets"))
              .build();
        }
      }
    }
    return getSearchMatrixOffsetsMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PointsStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PointsStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PointsStub>() {
        @java.lang.Override
        public PointsStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PointsStub(channel, callOptions);
        }
      };
    return PointsStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PointsBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PointsBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PointsBlockingStub>() {
        @java.lang.Override
        public PointsBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PointsBlockingStub(channel, callOptions);
        }
      };
    return PointsBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static PointsFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PointsFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PointsFutureStub>() {
        @java.lang.Override
        public PointsFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PointsFutureStub(channel, callOptions);
        }
      };
    return PointsFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     *Perform insert + updates on points. If a point with a given ID already exists - it will be overwritten.
     * </pre>
     */
    default void upsert(io.qdrant.client.grpc.Points.UpsertPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpsertMethod(), responseObserver);
    }

    /**
     * <pre>
     *Delete points
     * </pre>
     */
    default void delete(io.qdrant.client.grpc.Points.DeletePoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteMethod(), responseObserver);
    }

    /**
     * <pre>
     *Retrieve points
     * </pre>
     */
    default void get(io.qdrant.client.grpc.Points.GetPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.GetResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetMethod(), responseObserver);
    }

    /**
     * <pre>
     *Update named vectors for point
     * </pre>
     */
    default void updateVectors(io.qdrant.client.grpc.Points.UpdatePointVectors request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpdateVectorsMethod(), responseObserver);
    }

    /**
     * <pre>
     *Delete named vectors for points
     * </pre>
     */
    default void deleteVectors(io.qdrant.client.grpc.Points.DeletePointVectors request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteVectorsMethod(), responseObserver);
    }

    /**
     * <pre>
     *Set payload for points
     * </pre>
     */
    default void setPayload(io.qdrant.client.grpc.Points.SetPayloadPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetPayloadMethod(), responseObserver);
    }

    /**
     * <pre>
     *Overwrite payload for points
     * </pre>
     */
    default void overwritePayload(io.qdrant.client.grpc.Points.SetPayloadPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getOverwritePayloadMethod(), responseObserver);
    }

    /**
     * <pre>
     *Delete specified key payload for points
     * </pre>
     */
    default void deletePayload(io.qdrant.client.grpc.Points.DeletePayloadPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeletePayloadMethod(), responseObserver);
    }

    /**
     * <pre>
     *Remove all payload for specified points
     * </pre>
     */
    default void clearPayload(io.qdrant.client.grpc.Points.ClearPayloadPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getClearPayloadMethod(), responseObserver);
    }

    /**
     * <pre>
     *Create index for field in collection
     * </pre>
     */
    default void createFieldIndex(io.qdrant.client.grpc.Points.CreateFieldIndexCollection request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateFieldIndexMethod(), responseObserver);
    }

    /**
     * <pre>
     *Delete field index for collection
     * </pre>
     */
    default void deleteFieldIndex(io.qdrant.client.grpc.Points.DeleteFieldIndexCollection request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteFieldIndexMethod(), responseObserver);
    }

    /**
     * <pre>
     *Retrieve closest points based on vector similarity and given filtering conditions
     * </pre>
     */
    default void search(io.qdrant.client.grpc.Points.SearchPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.SearchResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSearchMethod(), responseObserver);
    }

    /**
     * <pre>
     *Retrieve closest points based on vector similarity and given filtering conditions
     * </pre>
     */
    default void searchBatch(io.qdrant.client.grpc.Points.SearchBatchPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.SearchBatchResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSearchBatchMethod(), responseObserver);
    }

    /**
     * <pre>
     *Retrieve closest points based on vector similarity and given filtering conditions, grouped by a given field
     * </pre>
     */
    default void searchGroups(io.qdrant.client.grpc.Points.SearchPointGroups request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.SearchGroupsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSearchGroupsMethod(), responseObserver);
    }

    /**
     * <pre>
     *Iterate over all or filtered points
     * </pre>
     */
    default void scroll(io.qdrant.client.grpc.Points.ScrollPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.ScrollResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getScrollMethod(), responseObserver);
    }

    /**
     * <pre>
     *Look for the points which are closer to stored positive examples and at the same time further to negative examples.
     * </pre>
     */
    default void recommend(io.qdrant.client.grpc.Points.RecommendPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.RecommendResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRecommendMethod(), responseObserver);
    }

    /**
     * <pre>
     *Look for the points which are closer to stored positive examples and at the same time further to negative examples.
     * </pre>
     */
    default void recommendBatch(io.qdrant.client.grpc.Points.RecommendBatchPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.RecommendBatchResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRecommendBatchMethod(), responseObserver);
    }

    /**
     * <pre>
     *Look for the points which are closer to stored positive examples and at the same time further to negative examples, grouped by a given field
     * </pre>
     */
    default void recommendGroups(io.qdrant.client.grpc.Points.RecommendPointGroups request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.RecommendGroupsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRecommendGroupsMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use context and a target to find the most similar points to the target, constrained by the context.
     *When using only the context (without a target), a special search - called context search - is performed where
     *pairs of points are used to generate a loss that guides the search towards the zone where
     *most positive examples overlap. This means that the score minimizes the scenario of
     *finding a point closer to a negative than to a positive part of a pair.
     *Since the score of a context relates to loss, the maximum score a point can get is 0.0,
     *and it becomes normal that many points can have a score of 0.0.
     *When using target (with or without context), the score behaves a little different: The 
     *integer part of the score represents the rank with respect to the context, while the
     *decimal part of the score relates to the distance to the target. The context part of the score for 
     *each pair is calculated +1 if the point is closer to a positive than to a negative part of a pair, 
     *and -1 otherwise.
     * </pre>
     */
    default void discover(io.qdrant.client.grpc.Points.DiscoverPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.DiscoverResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDiscoverMethod(), responseObserver);
    }

    /**
     * <pre>
     *Batch request points based on { positive, negative } pairs of examples, and/or a target
     * </pre>
     */
    default void discoverBatch(io.qdrant.client.grpc.Points.DiscoverBatchPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.DiscoverBatchResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDiscoverBatchMethod(), responseObserver);
    }

    /**
     * <pre>
     *Count points in collection with given filtering conditions
     * </pre>
     */
    default void count(io.qdrant.client.grpc.Points.CountPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.CountResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCountMethod(), responseObserver);
    }

    /**
     * <pre>
     *Perform multiple update operations in one request
     * </pre>
     */
    default void updateBatch(io.qdrant.client.grpc.Points.UpdateBatchPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.UpdateBatchResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpdateBatchMethod(), responseObserver);
    }

    /**
     * <pre>
     *Universally query points. This endpoint covers all capabilities of search, recommend, discover, filters. But also enables hybrid and multi-stage queries.
     * </pre>
     */
    default void query(io.qdrant.client.grpc.Points.QueryPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.QueryResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getQueryMethod(), responseObserver);
    }

    /**
     * <pre>
     *Universally query points in a batch fashion. This endpoint covers all capabilities of search, recommend, discover, filters. But also enables hybrid and multi-stage queries.
     * </pre>
     */
    default void queryBatch(io.qdrant.client.grpc.Points.QueryBatchPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.QueryBatchResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getQueryBatchMethod(), responseObserver);
    }

    /**
     * <pre>
     *Universally query points in a group fashion. This endpoint covers all capabilities of search, recommend, discover, filters. But also enables hybrid and multi-stage queries.
     * </pre>
     */
    default void queryGroups(io.qdrant.client.grpc.Points.QueryPointGroups request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.QueryGroupsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getQueryGroupsMethod(), responseObserver);
    }

    /**
     * <pre>
     *Perform facet counts. For each value in the field, count the number of points that have this value and match the conditions.
     * </pre>
     */
    default void facet(io.qdrant.client.grpc.Points.FacetCounts request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.FacetResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getFacetMethod(), responseObserver);
    }

    /**
     * <pre>
     *Compute distance matrix for sampled points with a pair based output format
     * </pre>
     */
    default void searchMatrixPairs(io.qdrant.client.grpc.Points.SearchMatrixPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.SearchMatrixPairsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSearchMatrixPairsMethod(), responseObserver);
    }

    /**
     * <pre>
     *Compute distance matrix for sampled points with an offset based output format
     * </pre>
     */
    default void searchMatrixOffsets(io.qdrant.client.grpc.Points.SearchMatrixPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.SearchMatrixOffsetsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSearchMatrixOffsetsMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service Points.
   */
  public static abstract class PointsImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return PointsGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service Points.
   */
  public static final class PointsStub
      extends io.grpc.stub.AbstractAsyncStub<PointsStub> {
    private PointsStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PointsStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PointsStub(channel, callOptions);
    }

    /**
     * <pre>
     *Perform insert + updates on points. If a point with a given ID already exists - it will be overwritten.
     * </pre>
     */
    public void upsert(io.qdrant.client.grpc.Points.UpsertPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpsertMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Delete points
     * </pre>
     */
    public void delete(io.qdrant.client.grpc.Points.DeletePoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Retrieve points
     * </pre>
     */
    public void get(io.qdrant.client.grpc.Points.GetPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.GetResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Update named vectors for point
     * </pre>
     */
    public void updateVectors(io.qdrant.client.grpc.Points.UpdatePointVectors request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateVectorsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Delete named vectors for points
     * </pre>
     */
    public void deleteVectors(io.qdrant.client.grpc.Points.DeletePointVectors request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteVectorsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Set payload for points
     * </pre>
     */
    public void setPayload(io.qdrant.client.grpc.Points.SetPayloadPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetPayloadMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Overwrite payload for points
     * </pre>
     */
    public void overwritePayload(io.qdrant.client.grpc.Points.SetPayloadPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getOverwritePayloadMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Delete specified key payload for points
     * </pre>
     */
    public void deletePayload(io.qdrant.client.grpc.Points.DeletePayloadPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeletePayloadMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Remove all payload for specified points
     * </pre>
     */
    public void clearPayload(io.qdrant.client.grpc.Points.ClearPayloadPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getClearPayloadMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Create index for field in collection
     * </pre>
     */
    public void createFieldIndex(io.qdrant.client.grpc.Points.CreateFieldIndexCollection request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateFieldIndexMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Delete field index for collection
     * </pre>
     */
    public void deleteFieldIndex(io.qdrant.client.grpc.Points.DeleteFieldIndexCollection request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteFieldIndexMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Retrieve closest points based on vector similarity and given filtering conditions
     * </pre>
     */
    public void search(io.qdrant.client.grpc.Points.SearchPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.SearchResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSearchMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Retrieve closest points based on vector similarity and given filtering conditions
     * </pre>
     */
    public void searchBatch(io.qdrant.client.grpc.Points.SearchBatchPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.SearchBatchResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSearchBatchMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Retrieve closest points based on vector similarity and given filtering conditions, grouped by a given field
     * </pre>
     */
    public void searchGroups(io.qdrant.client.grpc.Points.SearchPointGroups request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.SearchGroupsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSearchGroupsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Iterate over all or filtered points
     * </pre>
     */
    public void scroll(io.qdrant.client.grpc.Points.ScrollPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.ScrollResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getScrollMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Look for the points which are closer to stored positive examples and at the same time further to negative examples.
     * </pre>
     */
    public void recommend(io.qdrant.client.grpc.Points.RecommendPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.RecommendResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRecommendMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Look for the points which are closer to stored positive examples and at the same time further to negative examples.
     * </pre>
     */
    public void recommendBatch(io.qdrant.client.grpc.Points.RecommendBatchPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.RecommendBatchResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRecommendBatchMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Look for the points which are closer to stored positive examples and at the same time further to negative examples, grouped by a given field
     * </pre>
     */
    public void recommendGroups(io.qdrant.client.grpc.Points.RecommendPointGroups request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.RecommendGroupsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRecommendGroupsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use context and a target to find the most similar points to the target, constrained by the context.
     *When using only the context (without a target), a special search - called context search - is performed where
     *pairs of points are used to generate a loss that guides the search towards the zone where
     *most positive examples overlap. This means that the score minimizes the scenario of
     *finding a point closer to a negative than to a positive part of a pair.
     *Since the score of a context relates to loss, the maximum score a point can get is 0.0,
     *and it becomes normal that many points can have a score of 0.0.
     *When using target (with or without context), the score behaves a little different: The 
     *integer part of the score represents the rank with respect to the context, while the
     *decimal part of the score relates to the distance to the target. The context part of the score for 
     *each pair is calculated +1 if the point is closer to a positive than to a negative part of a pair, 
     *and -1 otherwise.
     * </pre>
     */
    public void discover(io.qdrant.client.grpc.Points.DiscoverPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.DiscoverResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDiscoverMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Batch request points based on { positive, negative } pairs of examples, and/or a target
     * </pre>
     */
    public void discoverBatch(io.qdrant.client.grpc.Points.DiscoverBatchPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.DiscoverBatchResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDiscoverBatchMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Count points in collection with given filtering conditions
     * </pre>
     */
    public void count(io.qdrant.client.grpc.Points.CountPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.CountResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Perform multiple update operations in one request
     * </pre>
     */
    public void updateBatch(io.qdrant.client.grpc.Points.UpdateBatchPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.UpdateBatchResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateBatchMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Universally query points. This endpoint covers all capabilities of search, recommend, discover, filters. But also enables hybrid and multi-stage queries.
     * </pre>
     */
    public void query(io.qdrant.client.grpc.Points.QueryPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.QueryResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getQueryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Universally query points in a batch fashion. This endpoint covers all capabilities of search, recommend, discover, filters. But also enables hybrid and multi-stage queries.
     * </pre>
     */
    public void queryBatch(io.qdrant.client.grpc.Points.QueryBatchPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.QueryBatchResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getQueryBatchMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Universally query points in a group fashion. This endpoint covers all capabilities of search, recommend, discover, filters. But also enables hybrid and multi-stage queries.
     * </pre>
     */
    public void queryGroups(io.qdrant.client.grpc.Points.QueryPointGroups request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.QueryGroupsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getQueryGroupsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Perform facet counts. For each value in the field, count the number of points that have this value and match the conditions.
     * </pre>
     */
    public void facet(io.qdrant.client.grpc.Points.FacetCounts request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.FacetResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getFacetMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Compute distance matrix for sampled points with a pair based output format
     * </pre>
     */
    public void searchMatrixPairs(io.qdrant.client.grpc.Points.SearchMatrixPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.SearchMatrixPairsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSearchMatrixPairsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Compute distance matrix for sampled points with an offset based output format
     * </pre>
     */
    public void searchMatrixOffsets(io.qdrant.client.grpc.Points.SearchMatrixPoints request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.SearchMatrixOffsetsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSearchMatrixOffsetsMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service Points.
   */
  public static final class PointsBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<PointsBlockingStub> {
    private PointsBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PointsBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PointsBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     *Perform insert + updates on points. If a point with a given ID already exists - it will be overwritten.
     * </pre>
     */
    public io.qdrant.client.grpc.Points.PointsOperationResponse upsert(io.qdrant.client.grpc.Points.UpsertPoints request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpsertMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Delete points
     * </pre>
     */
    public io.qdrant.client.grpc.Points.PointsOperationResponse delete(io.qdrant.client.grpc.Points.DeletePoints request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Retrieve points
     * </pre>
     */
    public io.qdrant.client.grpc.Points.GetResponse get(io.qdrant.client.grpc.Points.GetPoints request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Update named vectors for point
     * </pre>
     */
    public io.qdrant.client.grpc.Points.PointsOperationResponse updateVectors(io.qdrant.client.grpc.Points.UpdatePointVectors request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateVectorsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Delete named vectors for points
     * </pre>
     */
    public io.qdrant.client.grpc.Points.PointsOperationResponse deleteVectors(io.qdrant.client.grpc.Points.DeletePointVectors request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteVectorsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Set payload for points
     * </pre>
     */
    public io.qdrant.client.grpc.Points.PointsOperationResponse setPayload(io.qdrant.client.grpc.Points.SetPayloadPoints request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetPayloadMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Overwrite payload for points
     * </pre>
     */
    public io.qdrant.client.grpc.Points.PointsOperationResponse overwritePayload(io.qdrant.client.grpc.Points.SetPayloadPoints request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getOverwritePayloadMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Delete specified key payload for points
     * </pre>
     */
    public io.qdrant.client.grpc.Points.PointsOperationResponse deletePayload(io.qdrant.client.grpc.Points.DeletePayloadPoints request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeletePayloadMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Remove all payload for specified points
     * </pre>
     */
    public io.qdrant.client.grpc.Points.PointsOperationResponse clearPayload(io.qdrant.client.grpc.Points.ClearPayloadPoints request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getClearPayloadMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Create index for field in collection
     * </pre>
     */
    public io.qdrant.client.grpc.Points.PointsOperationResponse createFieldIndex(io.qdrant.client.grpc.Points.CreateFieldIndexCollection request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateFieldIndexMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Delete field index for collection
     * </pre>
     */
    public io.qdrant.client.grpc.Points.PointsOperationResponse deleteFieldIndex(io.qdrant.client.grpc.Points.DeleteFieldIndexCollection request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteFieldIndexMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Retrieve closest points based on vector similarity and given filtering conditions
     * </pre>
     */
    public io.qdrant.client.grpc.Points.SearchResponse search(io.qdrant.client.grpc.Points.SearchPoints request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSearchMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Retrieve closest points based on vector similarity and given filtering conditions
     * </pre>
     */
    public io.qdrant.client.grpc.Points.SearchBatchResponse searchBatch(io.qdrant.client.grpc.Points.SearchBatchPoints request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSearchBatchMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Retrieve closest points based on vector similarity and given filtering conditions, grouped by a given field
     * </pre>
     */
    public io.qdrant.client.grpc.Points.SearchGroupsResponse searchGroups(io.qdrant.client.grpc.Points.SearchPointGroups request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSearchGroupsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Iterate over all or filtered points
     * </pre>
     */
    public io.qdrant.client.grpc.Points.ScrollResponse scroll(io.qdrant.client.grpc.Points.ScrollPoints request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getScrollMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Look for the points which are closer to stored positive examples and at the same time further to negative examples.
     * </pre>
     */
    public io.qdrant.client.grpc.Points.RecommendResponse recommend(io.qdrant.client.grpc.Points.RecommendPoints request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRecommendMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Look for the points which are closer to stored positive examples and at the same time further to negative examples.
     * </pre>
     */
    public io.qdrant.client.grpc.Points.RecommendBatchResponse recommendBatch(io.qdrant.client.grpc.Points.RecommendBatchPoints request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRecommendBatchMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Look for the points which are closer to stored positive examples and at the same time further to negative examples, grouped by a given field
     * </pre>
     */
    public io.qdrant.client.grpc.Points.RecommendGroupsResponse recommendGroups(io.qdrant.client.grpc.Points.RecommendPointGroups request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRecommendGroupsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use context and a target to find the most similar points to the target, constrained by the context.
     *When using only the context (without a target), a special search - called context search - is performed where
     *pairs of points are used to generate a loss that guides the search towards the zone where
     *most positive examples overlap. This means that the score minimizes the scenario of
     *finding a point closer to a negative than to a positive part of a pair.
     *Since the score of a context relates to loss, the maximum score a point can get is 0.0,
     *and it becomes normal that many points can have a score of 0.0.
     *When using target (with or without context), the score behaves a little different: The 
     *integer part of the score represents the rank with respect to the context, while the
     *decimal part of the score relates to the distance to the target. The context part of the score for 
     *each pair is calculated +1 if the point is closer to a positive than to a negative part of a pair, 
     *and -1 otherwise.
     * </pre>
     */
    public io.qdrant.client.grpc.Points.DiscoverResponse discover(io.qdrant.client.grpc.Points.DiscoverPoints request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDiscoverMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Batch request points based on { positive, negative } pairs of examples, and/or a target
     * </pre>
     */
    public io.qdrant.client.grpc.Points.DiscoverBatchResponse discoverBatch(io.qdrant.client.grpc.Points.DiscoverBatchPoints request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDiscoverBatchMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Count points in collection with given filtering conditions
     * </pre>
     */
    public io.qdrant.client.grpc.Points.CountResponse count(io.qdrant.client.grpc.Points.CountPoints request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCountMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Perform multiple update operations in one request
     * </pre>
     */
    public io.qdrant.client.grpc.Points.UpdateBatchResponse updateBatch(io.qdrant.client.grpc.Points.UpdateBatchPoints request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateBatchMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Universally query points. This endpoint covers all capabilities of search, recommend, discover, filters. But also enables hybrid and multi-stage queries.
     * </pre>
     */
    public io.qdrant.client.grpc.Points.QueryResponse query(io.qdrant.client.grpc.Points.QueryPoints request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getQueryMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Universally query points in a batch fashion. This endpoint covers all capabilities of search, recommend, discover, filters. But also enables hybrid and multi-stage queries.
     * </pre>
     */
    public io.qdrant.client.grpc.Points.QueryBatchResponse queryBatch(io.qdrant.client.grpc.Points.QueryBatchPoints request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getQueryBatchMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Universally query points in a group fashion. This endpoint covers all capabilities of search, recommend, discover, filters. But also enables hybrid and multi-stage queries.
     * </pre>
     */
    public io.qdrant.client.grpc.Points.QueryGroupsResponse queryGroups(io.qdrant.client.grpc.Points.QueryPointGroups request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getQueryGroupsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Perform facet counts. For each value in the field, count the number of points that have this value and match the conditions.
     * </pre>
     */
    public io.qdrant.client.grpc.Points.FacetResponse facet(io.qdrant.client.grpc.Points.FacetCounts request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getFacetMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Compute distance matrix for sampled points with a pair based output format
     * </pre>
     */
    public io.qdrant.client.grpc.Points.SearchMatrixPairsResponse searchMatrixPairs(io.qdrant.client.grpc.Points.SearchMatrixPoints request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSearchMatrixPairsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Compute distance matrix for sampled points with an offset based output format
     * </pre>
     */
    public io.qdrant.client.grpc.Points.SearchMatrixOffsetsResponse searchMatrixOffsets(io.qdrant.client.grpc.Points.SearchMatrixPoints request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSearchMatrixOffsetsMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service Points.
   */
  public static final class PointsFutureStub
      extends io.grpc.stub.AbstractFutureStub<PointsFutureStub> {
    private PointsFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PointsFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PointsFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     *Perform insert + updates on points. If a point with a given ID already exists - it will be overwritten.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.PointsOperationResponse> upsert(
        io.qdrant.client.grpc.Points.UpsertPoints request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpsertMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Delete points
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.PointsOperationResponse> delete(
        io.qdrant.client.grpc.Points.DeletePoints request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Retrieve points
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.GetResponse> get(
        io.qdrant.client.grpc.Points.GetPoints request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Update named vectors for point
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.PointsOperationResponse> updateVectors(
        io.qdrant.client.grpc.Points.UpdatePointVectors request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateVectorsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Delete named vectors for points
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.PointsOperationResponse> deleteVectors(
        io.qdrant.client.grpc.Points.DeletePointVectors request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteVectorsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Set payload for points
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.PointsOperationResponse> setPayload(
        io.qdrant.client.grpc.Points.SetPayloadPoints request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetPayloadMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Overwrite payload for points
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.PointsOperationResponse> overwritePayload(
        io.qdrant.client.grpc.Points.SetPayloadPoints request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getOverwritePayloadMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Delete specified key payload for points
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.PointsOperationResponse> deletePayload(
        io.qdrant.client.grpc.Points.DeletePayloadPoints request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeletePayloadMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Remove all payload for specified points
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.PointsOperationResponse> clearPayload(
        io.qdrant.client.grpc.Points.ClearPayloadPoints request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getClearPayloadMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Create index for field in collection
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.PointsOperationResponse> createFieldIndex(
        io.qdrant.client.grpc.Points.CreateFieldIndexCollection request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateFieldIndexMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Delete field index for collection
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.PointsOperationResponse> deleteFieldIndex(
        io.qdrant.client.grpc.Points.DeleteFieldIndexCollection request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteFieldIndexMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Retrieve closest points based on vector similarity and given filtering conditions
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.SearchResponse> search(
        io.qdrant.client.grpc.Points.SearchPoints request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSearchMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Retrieve closest points based on vector similarity and given filtering conditions
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.SearchBatchResponse> searchBatch(
        io.qdrant.client.grpc.Points.SearchBatchPoints request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSearchBatchMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Retrieve closest points based on vector similarity and given filtering conditions, grouped by a given field
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.SearchGroupsResponse> searchGroups(
        io.qdrant.client.grpc.Points.SearchPointGroups request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSearchGroupsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Iterate over all or filtered points
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.ScrollResponse> scroll(
        io.qdrant.client.grpc.Points.ScrollPoints request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getScrollMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Look for the points which are closer to stored positive examples and at the same time further to negative examples.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.RecommendResponse> recommend(
        io.qdrant.client.grpc.Points.RecommendPoints request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRecommendMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Look for the points which are closer to stored positive examples and at the same time further to negative examples.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.RecommendBatchResponse> recommendBatch(
        io.qdrant.client.grpc.Points.RecommendBatchPoints request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRecommendBatchMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Look for the points which are closer to stored positive examples and at the same time further to negative examples, grouped by a given field
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.RecommendGroupsResponse> recommendGroups(
        io.qdrant.client.grpc.Points.RecommendPointGroups request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRecommendGroupsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use context and a target to find the most similar points to the target, constrained by the context.
     *When using only the context (without a target), a special search - called context search - is performed where
     *pairs of points are used to generate a loss that guides the search towards the zone where
     *most positive examples overlap. This means that the score minimizes the scenario of
     *finding a point closer to a negative than to a positive part of a pair.
     *Since the score of a context relates to loss, the maximum score a point can get is 0.0,
     *and it becomes normal that many points can have a score of 0.0.
     *When using target (with or without context), the score behaves a little different: The 
     *integer part of the score represents the rank with respect to the context, while the
     *decimal part of the score relates to the distance to the target. The context part of the score for 
     *each pair is calculated +1 if the point is closer to a positive than to a negative part of a pair, 
     *and -1 otherwise.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.DiscoverResponse> discover(
        io.qdrant.client.grpc.Points.DiscoverPoints request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDiscoverMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Batch request points based on { positive, negative } pairs of examples, and/or a target
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.DiscoverBatchResponse> discoverBatch(
        io.qdrant.client.grpc.Points.DiscoverBatchPoints request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDiscoverBatchMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Count points in collection with given filtering conditions
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.CountResponse> count(
        io.qdrant.client.grpc.Points.CountPoints request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCountMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Perform multiple update operations in one request
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.UpdateBatchResponse> updateBatch(
        io.qdrant.client.grpc.Points.UpdateBatchPoints request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateBatchMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Universally query points. This endpoint covers all capabilities of search, recommend, discover, filters. But also enables hybrid and multi-stage queries.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.QueryResponse> query(
        io.qdrant.client.grpc.Points.QueryPoints request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getQueryMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Universally query points in a batch fashion. This endpoint covers all capabilities of search, recommend, discover, filters. But also enables hybrid and multi-stage queries.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.QueryBatchResponse> queryBatch(
        io.qdrant.client.grpc.Points.QueryBatchPoints request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getQueryBatchMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Universally query points in a group fashion. This endpoint covers all capabilities of search, recommend, discover, filters. But also enables hybrid and multi-stage queries.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.QueryGroupsResponse> queryGroups(
        io.qdrant.client.grpc.Points.QueryPointGroups request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getQueryGroupsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Perform facet counts. For each value in the field, count the number of points that have this value and match the conditions.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.FacetResponse> facet(
        io.qdrant.client.grpc.Points.FacetCounts request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getFacetMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Compute distance matrix for sampled points with a pair based output format
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.SearchMatrixPairsResponse> searchMatrixPairs(
        io.qdrant.client.grpc.Points.SearchMatrixPoints request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSearchMatrixPairsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Compute distance matrix for sampled points with an offset based output format
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.SearchMatrixOffsetsResponse> searchMatrixOffsets(
        io.qdrant.client.grpc.Points.SearchMatrixPoints request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSearchMatrixOffsetsMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_UPSERT = 0;
  private static final int METHODID_DELETE = 1;
  private static final int METHODID_GET = 2;
  private static final int METHODID_UPDATE_VECTORS = 3;
  private static final int METHODID_DELETE_VECTORS = 4;
  private static final int METHODID_SET_PAYLOAD = 5;
  private static final int METHODID_OVERWRITE_PAYLOAD = 6;
  private static final int METHODID_DELETE_PAYLOAD = 7;
  private static final int METHODID_CLEAR_PAYLOAD = 8;
  private static final int METHODID_CREATE_FIELD_INDEX = 9;
  private static final int METHODID_DELETE_FIELD_INDEX = 10;
  private static final int METHODID_SEARCH = 11;
  private static final int METHODID_SEARCH_BATCH = 12;
  private static final int METHODID_SEARCH_GROUPS = 13;
  private static final int METHODID_SCROLL = 14;
  private static final int METHODID_RECOMMEND = 15;
  private static final int METHODID_RECOMMEND_BATCH = 16;
  private static final int METHODID_RECOMMEND_GROUPS = 17;
  private static final int METHODID_DISCOVER = 18;
  private static final int METHODID_DISCOVER_BATCH = 19;
  private static final int METHODID_COUNT = 20;
  private static final int METHODID_UPDATE_BATCH = 21;
  private static final int METHODID_QUERY = 22;
  private static final int METHODID_QUERY_BATCH = 23;
  private static final int METHODID_QUERY_GROUPS = 24;
  private static final int METHODID_FACET = 25;
  private static final int METHODID_SEARCH_MATRIX_PAIRS = 26;
  private static final int METHODID_SEARCH_MATRIX_OFFSETS = 27;

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
        case METHODID_UPSERT:
          serviceImpl.upsert((io.qdrant.client.grpc.Points.UpsertPoints) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse>) responseObserver);
          break;
        case METHODID_DELETE:
          serviceImpl.delete((io.qdrant.client.grpc.Points.DeletePoints) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse>) responseObserver);
          break;
        case METHODID_GET:
          serviceImpl.get((io.qdrant.client.grpc.Points.GetPoints) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.GetResponse>) responseObserver);
          break;
        case METHODID_UPDATE_VECTORS:
          serviceImpl.updateVectors((io.qdrant.client.grpc.Points.UpdatePointVectors) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse>) responseObserver);
          break;
        case METHODID_DELETE_VECTORS:
          serviceImpl.deleteVectors((io.qdrant.client.grpc.Points.DeletePointVectors) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse>) responseObserver);
          break;
        case METHODID_SET_PAYLOAD:
          serviceImpl.setPayload((io.qdrant.client.grpc.Points.SetPayloadPoints) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse>) responseObserver);
          break;
        case METHODID_OVERWRITE_PAYLOAD:
          serviceImpl.overwritePayload((io.qdrant.client.grpc.Points.SetPayloadPoints) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse>) responseObserver);
          break;
        case METHODID_DELETE_PAYLOAD:
          serviceImpl.deletePayload((io.qdrant.client.grpc.Points.DeletePayloadPoints) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse>) responseObserver);
          break;
        case METHODID_CLEAR_PAYLOAD:
          serviceImpl.clearPayload((io.qdrant.client.grpc.Points.ClearPayloadPoints) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse>) responseObserver);
          break;
        case METHODID_CREATE_FIELD_INDEX:
          serviceImpl.createFieldIndex((io.qdrant.client.grpc.Points.CreateFieldIndexCollection) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse>) responseObserver);
          break;
        case METHODID_DELETE_FIELD_INDEX:
          serviceImpl.deleteFieldIndex((io.qdrant.client.grpc.Points.DeleteFieldIndexCollection) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.PointsOperationResponse>) responseObserver);
          break;
        case METHODID_SEARCH:
          serviceImpl.search((io.qdrant.client.grpc.Points.SearchPoints) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.SearchResponse>) responseObserver);
          break;
        case METHODID_SEARCH_BATCH:
          serviceImpl.searchBatch((io.qdrant.client.grpc.Points.SearchBatchPoints) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.SearchBatchResponse>) responseObserver);
          break;
        case METHODID_SEARCH_GROUPS:
          serviceImpl.searchGroups((io.qdrant.client.grpc.Points.SearchPointGroups) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.SearchGroupsResponse>) responseObserver);
          break;
        case METHODID_SCROLL:
          serviceImpl.scroll((io.qdrant.client.grpc.Points.ScrollPoints) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.ScrollResponse>) responseObserver);
          break;
        case METHODID_RECOMMEND:
          serviceImpl.recommend((io.qdrant.client.grpc.Points.RecommendPoints) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.RecommendResponse>) responseObserver);
          break;
        case METHODID_RECOMMEND_BATCH:
          serviceImpl.recommendBatch((io.qdrant.client.grpc.Points.RecommendBatchPoints) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.RecommendBatchResponse>) responseObserver);
          break;
        case METHODID_RECOMMEND_GROUPS:
          serviceImpl.recommendGroups((io.qdrant.client.grpc.Points.RecommendPointGroups) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.RecommendGroupsResponse>) responseObserver);
          break;
        case METHODID_DISCOVER:
          serviceImpl.discover((io.qdrant.client.grpc.Points.DiscoverPoints) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.DiscoverResponse>) responseObserver);
          break;
        case METHODID_DISCOVER_BATCH:
          serviceImpl.discoverBatch((io.qdrant.client.grpc.Points.DiscoverBatchPoints) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.DiscoverBatchResponse>) responseObserver);
          break;
        case METHODID_COUNT:
          serviceImpl.count((io.qdrant.client.grpc.Points.CountPoints) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.CountResponse>) responseObserver);
          break;
        case METHODID_UPDATE_BATCH:
          serviceImpl.updateBatch((io.qdrant.client.grpc.Points.UpdateBatchPoints) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.UpdateBatchResponse>) responseObserver);
          break;
        case METHODID_QUERY:
          serviceImpl.query((io.qdrant.client.grpc.Points.QueryPoints) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.QueryResponse>) responseObserver);
          break;
        case METHODID_QUERY_BATCH:
          serviceImpl.queryBatch((io.qdrant.client.grpc.Points.QueryBatchPoints) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.QueryBatchResponse>) responseObserver);
          break;
        case METHODID_QUERY_GROUPS:
          serviceImpl.queryGroups((io.qdrant.client.grpc.Points.QueryPointGroups) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.QueryGroupsResponse>) responseObserver);
          break;
        case METHODID_FACET:
          serviceImpl.facet((io.qdrant.client.grpc.Points.FacetCounts) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.FacetResponse>) responseObserver);
          break;
        case METHODID_SEARCH_MATRIX_PAIRS:
          serviceImpl.searchMatrixPairs((io.qdrant.client.grpc.Points.SearchMatrixPoints) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.SearchMatrixPairsResponse>) responseObserver);
          break;
        case METHODID_SEARCH_MATRIX_OFFSETS:
          serviceImpl.searchMatrixOffsets((io.qdrant.client.grpc.Points.SearchMatrixPoints) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.SearchMatrixOffsetsResponse>) responseObserver);
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
          getUpsertMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.UpsertPoints,
              io.qdrant.client.grpc.Points.PointsOperationResponse>(
                service, METHODID_UPSERT)))
        .addMethod(
          getDeleteMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.DeletePoints,
              io.qdrant.client.grpc.Points.PointsOperationResponse>(
                service, METHODID_DELETE)))
        .addMethod(
          getGetMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.GetPoints,
              io.qdrant.client.grpc.Points.GetResponse>(
                service, METHODID_GET)))
        .addMethod(
          getUpdateVectorsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.UpdatePointVectors,
              io.qdrant.client.grpc.Points.PointsOperationResponse>(
                service, METHODID_UPDATE_VECTORS)))
        .addMethod(
          getDeleteVectorsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.DeletePointVectors,
              io.qdrant.client.grpc.Points.PointsOperationResponse>(
                service, METHODID_DELETE_VECTORS)))
        .addMethod(
          getSetPayloadMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.SetPayloadPoints,
              io.qdrant.client.grpc.Points.PointsOperationResponse>(
                service, METHODID_SET_PAYLOAD)))
        .addMethod(
          getOverwritePayloadMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.SetPayloadPoints,
              io.qdrant.client.grpc.Points.PointsOperationResponse>(
                service, METHODID_OVERWRITE_PAYLOAD)))
        .addMethod(
          getDeletePayloadMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.DeletePayloadPoints,
              io.qdrant.client.grpc.Points.PointsOperationResponse>(
                service, METHODID_DELETE_PAYLOAD)))
        .addMethod(
          getClearPayloadMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.ClearPayloadPoints,
              io.qdrant.client.grpc.Points.PointsOperationResponse>(
                service, METHODID_CLEAR_PAYLOAD)))
        .addMethod(
          getCreateFieldIndexMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.CreateFieldIndexCollection,
              io.qdrant.client.grpc.Points.PointsOperationResponse>(
                service, METHODID_CREATE_FIELD_INDEX)))
        .addMethod(
          getDeleteFieldIndexMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.DeleteFieldIndexCollection,
              io.qdrant.client.grpc.Points.PointsOperationResponse>(
                service, METHODID_DELETE_FIELD_INDEX)))
        .addMethod(
          getSearchMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.SearchPoints,
              io.qdrant.client.grpc.Points.SearchResponse>(
                service, METHODID_SEARCH)))
        .addMethod(
          getSearchBatchMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.SearchBatchPoints,
              io.qdrant.client.grpc.Points.SearchBatchResponse>(
                service, METHODID_SEARCH_BATCH)))
        .addMethod(
          getSearchGroupsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.SearchPointGroups,
              io.qdrant.client.grpc.Points.SearchGroupsResponse>(
                service, METHODID_SEARCH_GROUPS)))
        .addMethod(
          getScrollMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.ScrollPoints,
              io.qdrant.client.grpc.Points.ScrollResponse>(
                service, METHODID_SCROLL)))
        .addMethod(
          getRecommendMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.RecommendPoints,
              io.qdrant.client.grpc.Points.RecommendResponse>(
                service, METHODID_RECOMMEND)))
        .addMethod(
          getRecommendBatchMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.RecommendBatchPoints,
              io.qdrant.client.grpc.Points.RecommendBatchResponse>(
                service, METHODID_RECOMMEND_BATCH)))
        .addMethod(
          getRecommendGroupsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.RecommendPointGroups,
              io.qdrant.client.grpc.Points.RecommendGroupsResponse>(
                service, METHODID_RECOMMEND_GROUPS)))
        .addMethod(
          getDiscoverMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.DiscoverPoints,
              io.qdrant.client.grpc.Points.DiscoverResponse>(
                service, METHODID_DISCOVER)))
        .addMethod(
          getDiscoverBatchMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.DiscoverBatchPoints,
              io.qdrant.client.grpc.Points.DiscoverBatchResponse>(
                service, METHODID_DISCOVER_BATCH)))
        .addMethod(
          getCountMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.CountPoints,
              io.qdrant.client.grpc.Points.CountResponse>(
                service, METHODID_COUNT)))
        .addMethod(
          getUpdateBatchMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.UpdateBatchPoints,
              io.qdrant.client.grpc.Points.UpdateBatchResponse>(
                service, METHODID_UPDATE_BATCH)))
        .addMethod(
          getQueryMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.QueryPoints,
              io.qdrant.client.grpc.Points.QueryResponse>(
                service, METHODID_QUERY)))
        .addMethod(
          getQueryBatchMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.QueryBatchPoints,
              io.qdrant.client.grpc.Points.QueryBatchResponse>(
                service, METHODID_QUERY_BATCH)))
        .addMethod(
          getQueryGroupsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.QueryPointGroups,
              io.qdrant.client.grpc.Points.QueryGroupsResponse>(
                service, METHODID_QUERY_GROUPS)))
        .addMethod(
          getFacetMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.FacetCounts,
              io.qdrant.client.grpc.Points.FacetResponse>(
                service, METHODID_FACET)))
        .addMethod(
          getSearchMatrixPairsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.SearchMatrixPoints,
              io.qdrant.client.grpc.Points.SearchMatrixPairsResponse>(
                service, METHODID_SEARCH_MATRIX_PAIRS)))
        .addMethod(
          getSearchMatrixOffsetsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Points.SearchMatrixPoints,
              io.qdrant.client.grpc.Points.SearchMatrixOffsetsResponse>(
                service, METHODID_SEARCH_MATRIX_OFFSETS)))
        .build();
  }

  private static abstract class PointsBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    PointsBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.qdrant.client.grpc.PointsService.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Points");
    }
  }

  private static final class PointsFileDescriptorSupplier
      extends PointsBaseDescriptorSupplier {
    PointsFileDescriptorSupplier() {}
  }

  private static final class PointsMethodDescriptorSupplier
      extends PointsBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    PointsMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (PointsGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new PointsFileDescriptorSupplier())
              .addMethod(getUpsertMethod())
              .addMethod(getDeleteMethod())
              .addMethod(getGetMethod())
              .addMethod(getUpdateVectorsMethod())
              .addMethod(getDeleteVectorsMethod())
              .addMethod(getSetPayloadMethod())
              .addMethod(getOverwritePayloadMethod())
              .addMethod(getDeletePayloadMethod())
              .addMethod(getClearPayloadMethod())
              .addMethod(getCreateFieldIndexMethod())
              .addMethod(getDeleteFieldIndexMethod())
              .addMethod(getSearchMethod())
              .addMethod(getSearchBatchMethod())
              .addMethod(getSearchGroupsMethod())
              .addMethod(getScrollMethod())
              .addMethod(getRecommendMethod())
              .addMethod(getRecommendBatchMethod())
              .addMethod(getRecommendGroupsMethod())
              .addMethod(getDiscoverMethod())
              .addMethod(getDiscoverBatchMethod())
              .addMethod(getCountMethod())
              .addMethod(getUpdateBatchMethod())
              .addMethod(getQueryMethod())
              .addMethod(getQueryBatchMethod())
              .addMethod(getQueryGroupsMethod())
              .addMethod(getFacetMethod())
              .addMethod(getSearchMatrixPairsMethod())
              .addMethod(getSearchMatrixOffsetsMethod())
              .build();
        }
      }
    }
    return result;
  }
}
