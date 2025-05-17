(ns lambda.codec
  (:import
   (java.io InputStream
            ByteArrayOutputStream
            ByteArrayInputStream)
   (java.util Base64)
   (java.util.zip GZIPOutputStream
                  GZIPInputStream)))


(defn b64-decode ^bytes [^bytes input]
  (.decode (Base64/getDecoder) input))


(defn b64-encode ^bytes [^bytes input]
  (.encode (Base64/getEncoder) input))


(defn bytes->str
  (^String [^bytes input]
   (new String input))

  (^String [^bytes input ^String encoding]
   (new String input encoding)))


(defn str->bytes
  (^bytes [^String input]
   (.getBytes input))

  (^bytes [^String input ^String encoding]
   (.getBytes input encoding)))


(defn bytes->gzip ^bytes [^bytes ba]
  (let [baos (new ByteArrayOutputStream)]
    (with-open [out (new GZIPOutputStream baos)
                in (new ByteArrayInputStream ba)]
      (.transferTo in out))
    (.toByteArray baos)))


(defn gzip->bytes ^bytes [^bytes ba]
  (let [baos (new ByteArrayOutputStream)]
    (with-open [in (new GZIPInputStream (new ByteArrayInputStream ba))]
      (.transferTo in baos))
    (.toByteArray baos)))


(defn read-bytes ^bytes [^InputStream in]
  (.readAllBytes in))
