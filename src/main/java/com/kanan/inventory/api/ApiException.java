package com.kanan.inventory.api;
public class ApiException extends RuntimeException {
    private final int status;
    public ApiException(int status,String message){super(message);this.status=status;}
    public int status(){return status;}
    public static ApiException badRequest(String m){return new ApiException(400,m);}
    public static ApiException unauthorized(){return new ApiException(401,"Valid HTTP Basic credentials are required");}
    public static ApiException forbidden(){return new ApiException(403,"You do not have permission to perform this action");}
    public static ApiException methodNotAllowed(){return new ApiException(405,"Method not allowed for this endpoint");}
    public static ApiException notFound(){return new ApiException(404,"Resource not found");}
    public static ApiException conflict(String m){return new ApiException(409,m);}
    public static ApiException internal(){return new ApiException(500,"An unexpected server error occurred");}
}
