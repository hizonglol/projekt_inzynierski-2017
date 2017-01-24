#ifndef TOOLS_H
#define TOOLS_H

//C++ libs
#include <iostream>
#include <fstream>
#include <sstream>
#include <string>
#include <random>
#include <memory>
#include <regex>
//C libs
#include <cstdlib>
#include <cstring>
//Other libraries
#include "fcgio.h"
#include <curl/curl.h>
//Headers
#include "base64.h"
#include "http_resp.h"
#include "qsp.h"
#include "timestamp.h"

#define VERIFICATION_LOG "/var/tmp/Haszowki/WP_verification.log";
#define ANSWER_LOG "/var/tmp/Haszowki/WP_answer.log"

// URL of GoogleApiUrl
static const std::string GoogleApiUrl = "https://www.googleapis.com/androidcheck/v1/attestations/verify?key=AIzaSyBtszAKi5zzBZNChvREDwFpl9g4a2LOriA";
// Maximum number of bytes allowed to be read from stdin
static const unsigned long STDIN_MAX = 1000000;

/**
 * Note this is not thread safe due to the static allocation of the
 * content_buffer.
 */
const std::string getRequestContent(const FCGX_Request& _Request)
{
  char* content_length_str = FCGX_GetParam("CONTENT_LENGTH", _Request.envp);
  unsigned long content_length = STDIN_MAX;

  if (content_length_str) {
    content_length = strtol(content_length_str, &content_length_str, 10);
    if (*content_length_str) {
      std::cerr << "Can't Parse 'CONTENT_LENGTH='"
                << FCGX_GetParam("CONTENT_LENGTH", _Request.envp)
                << "'. Consuming stdin up to " << STDIN_MAX << std::endl;
    }

    if (content_length > STDIN_MAX) {
      content_length = STDIN_MAX;
    }
  }
  else {
    // Do not read from stdin if CONTENT_LENGTH is missing
    content_length = 0;
  }

  char* content_buffer = new char[content_length];
  std::cin.read(content_buffer, content_length);
  content_length = std::cin.gcount();

  // Chew up any remaining stdin - this shouldn't be necessary
  // but is because mod_fastcgi doesn't handle it correctly.

  // ignore() doesn't set the eof bit in some versions of glibc++
  // so use gcount() instead of eof()...
  do {
    std::cin.ignore(1024);
  }
  while (std::cin.gcount() == 1024);

  std::string content(content_buffer, content_length);
  delete [] content_buffer;
  return content;
}

const bool checkHttps(bool _HttpsOn, const FCGX_Request& _Request)
{
  const char* https = FCGX_GetParam("HTTPS", _Request.envp);
  if (https != nullptr) {
    if (strcmp(https, "on"));
    _HttpsOn = true;
    return true;
  }
  else {
    _HttpsOn = false;
    return false;
  }
}

const std::string checkVerificationStatus(const std::string _SessionID2)
{

  std::ifstream File("/var/tmp/Haszowki/WP_verification.log", std::ios::in);

  if (File.good()) {

    std::string Buffer;
    std::string TempSessionID2, TempNonce, TempVerifStatus;
    std::string VerifStatus;

    while (getline(File, Buffer)) {

      std::istringstream ISStr(Buffer);
      while (ISStr >> TempSessionID2 >> TempNonce >> TempVerifStatus) {

        if (!_SessionID2.compare(TempSessionID2)) {
          VerifStatus = TempVerifStatus;
        }
      }
    }

    return VerifStatus;
  }

  return "NoInfo";
}

static const char charset[] =
  "0123456789"
  "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
  "abcdefghijklmnopqrstuvwxyz";

const std::string generateNonce()
{

  std::random_device Engine;
  std::string Nonce;
  unsigned char byte_array[24];

  for (int i = 0; i<24; ++i) {
    byte_array[i] = charset[Engine() % (sizeof(charset) - 1)];
  }

  Nonce = base64_encode(byte_array, 24);

  return Nonce;
}

const bool buildJSONmessage(const std::string& _RawMessage, std::string& _JSONmessage)
{

  _JSONmessage = "{ \"signedAttestation\": \"" + _RawMessage + "\" }";

  return true;
}

const bool decodeAttestation(const std::string _Attestation, std::string& _JSONdata, const std::string& _Header = "", const std::string& _Signature = "")
{
  int counter = 0;

  std::istringstream ISStr(_Attestation);
  std::string Buffer;
  std::vector<std::string> VectorBuffer;
  while (std::getline(ISStr, Buffer, '.')) {
    if (!Buffer.empty()) {
      VectorBuffer.push_back(Buffer);
      ++counter;
    }
  }

  if (counter == 3) {
    _JSONdata = base64_decode(VectorBuffer[1]);
    return true;
  }
  else {
    return false;
  }
}

struct curl_fetch_st {
  char* payload;
  size_t size;
};

static int callback(const char* _in, std::size_t _size, std::size_t _num, std::string* _out)
{
  const static int total_bytes(_size * _num);
  _out->append(_in, total_bytes);
  return total_bytes;
}

const bool checkAttestationAuthenticity(int& _ReturnCode, const std::string& _GoogleApiUrl, const std::string& _Attestation, std::string& _AttestationAuthenticity)
{
  CURL* Curl;
  CURLcode Res;
  curl_global_init(CURL_GLOBAL_ALL);

  struct curl_slist* headers = NULL;
  headers = curl_slist_append(headers, "Accept: application/json");
  headers = curl_slist_append(headers, "Content-Type: application/json");

  Curl = curl_easy_init();

  if (Curl) {
    std::unique_ptr<std::string> DownloadedResponse(new std::string());

    curl_easy_setopt(Curl, CURLOPT_CUSTOMREQUEST, "POST");
    curl_easy_setopt(Curl, CURLOPT_HTTPHEADER, headers);
    curl_easy_setopt(Curl, CURLOPT_POSTFIELDS, _Attestation.c_str());

    curl_easy_setopt(Curl, CURLOPT_URL, _GoogleApiUrl.c_str());
    curl_easy_setopt(Curl, CURLOPT_WRITEFUNCTION, callback);
    curl_easy_setopt(Curl, CURLOPT_WRITEDATA, DownloadedResponse.get());
    curl_easy_setopt(Curl, CURLOPT_USERAGENT, "libcurl-agent/1.0");
    curl_easy_setopt(Curl, CURLOPT_TIMEOUT, 5);
    curl_easy_setopt(Curl, CURLOPT_VERBOSE, 1);
    curl_easy_setopt(Curl, CURLOPT_FOLLOWLOCATION, 1);
    curl_easy_setopt(Curl, CURLOPT_MAXREDIRS, 1);
    Res = curl_easy_perform(Curl);


    curl_easy_getinfo(Curl, CURLINFO_RESPONSE_CODE, &_ReturnCode);

    curl_easy_cleanup(Curl);

    curl_slist_free_all(headers);

    if (CURLE_OK == Res) {
      _AttestationAuthenticity = *DownloadedResponse;
      return true;
    }
    else if (CURLE_OK != Res) {
      return false;
    }
  }

  return false;
}

void taskFCGI(FCGX_Request* _pRequest)
{

  // Make timestamp
  std::string Timestamp = makeTimestamp();
  // Main classes
  const std::string LogFileName = VERIFICATION_LOG;
  std::ofstream LogFile;
  std::string Content;
  // Main variables
  bool https_on(false);
  // Request info
  std::string PeerAddress;
  std::string ReqMethod;
  std::string QueryString;
  // Student info
  std::string SessionID2;
  // Creating pointers for stdio
  std::streambuf* cin_streambuf  = std::cin.rdbuf();
  std::streambuf* cout_streambuf = std::cout.rdbuf();
  std::streambuf* cerr_streambuf = std::cerr.rdbuf();
  // Assigning pointers to stdio of our request
  fcgi_streambuf cin_fcgi_streambuf((*_pRequest).in);
  fcgi_streambuf cout_fcgi_streambuf((*_pRequest).out);
  fcgi_streambuf cerr_fcgi_streambuf((*_pRequest).err);
  // Attaching stdio to fcgi stdio
  std::cin.rdbuf(&cin_fcgi_streambuf);
  std::cout.rdbuf(&cout_fcgi_streambuf);
  std::cerr.rdbuf(&cerr_fcgi_streambuf);
  // Although FastCGI supports writing before reading,
  // many http clients (browsers) don't support it (so
  // the connection deadlocks until a timeout expires!).
  Content = getRequestContent(*_pRequest);
  // Checking if secure connection
  checkHttps(https_on, *_pRequest);
  // Get data about connection
  PeerAddress = (std::string)FCGX_GetParam("REMOTE_ADDR", (*_pRequest).envp);
  ReqMethod = (std::string)FCGX_GetParam("REQUEST_METHOD", (*_pRequest).envp);
  QueryString = (std::string)FCGX_GetParam("QUERY_STRING", (*_pRequest).envp);

  LogFile.open(LogFileName, std::ios::out | std::ios::app);
  if (LogFile.is_open()) {
    // Inform client to expect XML
    std::cout << http_responses::makeHeader("text", "xml");

    if (ReqMethod == "GET") {

      // Generating and sending nonce
      // Writing down nonce and client's sessionID2
      qsp::extractSessionID2(QueryString, SessionID2);
      std::string Nonce = generateNonce();
      LogFile << Timestamp << " ";
      LogFile << SessionID2 << " ";
      LogFile << Nonce << " ";
      LogFile << "NonceSentToClient";
      LogFile << "\n";
      std::cout << http_responses::makeNonce(Nonce);
    }
    else if (ReqMethod == "POST") {

      // Receiving attestation
      // Writing down client's nonce and sessionID2
      qsp::extractSessionID2(QueryString, SessionID2);
      std::string Nonce;
      qsp::extractNonce(QueryString, Nonce);
      LogFile << Timestamp << " ";
      LogFile << SessionID2 << " ";
      LogFile << Nonce << " ";
      LogFile << "AttestationReceivedFromClient" << " " << Content;
      LogFile << "\n";
      std::cout << http_responses::makeNonce("AttestationReceived");

      // Verifying attestation authenticity
      // Writing down saved client's nonce and sessionID2
      int CurlCode;
      std::string JSONmessage;
      std::string CurlReply;
      buildJSONmessage(Content, JSONmessage);
      checkAttestationAuthenticity(CurlCode, GoogleApiUrl, JSONmessage, CurlReply);
      // Make timestamp
      Timestamp = makeTimestamp();
      LogFile << Timestamp << " ";
      LogFile << SessionID2 << " ";
      LogFile << Nonce << " ";
      LogFile << "AttestationAuthenticityVerified" << " ";
      LogFile << CurlReply;
      LogFile << "\n";
      //std::cout << http_responses::makeNonce("AttestationAuthenticityVerified");

      // Decoding attestation
      std::string JSONdata;
      decodeAttestation(Content, JSONdata);
      // Make timestamp
      Timestamp = makeTimestamp();
      LogFile << Timestamp << " ";
      LogFile << SessionID2 << " ";
      LogFile << Nonce << " ";
      LogFile << "AttestationDecoded" << " " << JSONdata;
      LogFile << "\n";

    }

    LogFile.close();
  }
  else {
    std::cout << http_responses::makeHeader("text", "xml");
    std::cout << http_responses::makeNonce("NULL");
  }

  std::cin.rdbuf(cin_streambuf);
  std::cout.rdbuf(cout_streambuf);
  std::cerr.rdbuf(cerr_streambuf);
}

FCGX_Request* newRequest()
{
  FCGX_Request* Request = new FCGX_Request();
  FCGX_InitRequest(Request, 0, 0);
  return Request;
}

#endif
