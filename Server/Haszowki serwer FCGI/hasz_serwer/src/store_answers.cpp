// VERSION 1.0.5

//C++ libs
#include <fstream>
#include <iostream>
#include <string>
//Other libraries
#include "fcgio.h"
#include <curl/curl.h>
//Headers
#include "base64.h"
#include "tools.h"
#include "http_resp.h"
#include "qsp.h"
#include "timestamp.h"

static const std::string LogFileName = ANSWER_LOG;

int main(void)
{
  // Main classes
  std::ofstream LogFile;
  std::string Content;
  std::string Timestamp;
  // Main variables
  bool https_on(false);
  // Request info
  std::string PeerAddress;
  std::string ReqMethod;
  std::string QueryString;
  // Creating pointers for stdio
  std::streambuf* cin_streambuf  = std::cin.rdbuf();
  std::streambuf* cout_streambuf = std::cout.rdbuf();
  std::streambuf* cerr_streambuf = std::cerr.rdbuf();
  // FCGI request class
  FCGX_Request Request;
  // Initializing FCGI
  FCGX_Init();
  FCGX_InitRequest(&Request, 0, 0);

  while (FCGX_Accept_r(&Request) >= 0) {
    // Make timestamp
    Timestamp = makeTimestamp();
    // Assigning pointers to stdio of our request
    fcgi_streambuf cin_fcgi_streambuf(Request.in);
    fcgi_streambuf cout_fcgi_streambuf(Request.out);
    fcgi_streambuf cerr_fcgi_streambuf(Request.err);
    // Attaching stdio to fcgi stdio
    std::cin.rdbuf(&cin_fcgi_streambuf);
    std::cout.rdbuf(&cout_fcgi_streambuf);
    std::cerr.rdbuf(&cerr_fcgi_streambuf);
    // Although FastCGI supports writing before reading,
    // many http clients (browsers) don't support it (so
    // the connection deadlocks until a timeout expires!).
    Content = getRequestContent(Request);
    // Checking if secure connection
    checkHttps(https_on, Request);
    // Get data about connection
    PeerAddress = (std::string)FCGX_GetParam("REMOTE_ADDR", Request.envp);
    ReqMethod = (std::string)FCGX_GetParam("REQUEST_METHOD", Request.envp);
    QueryString = (std::string)FCGX_GetParam("QUERY_STRING", Request.envp);
    // Opening log file
    LogFile.open(LogFileName, std::ios::out | std::ios::app);
    if (LogFile.is_open()) {
      // REQUEST HANDLING
      // Inform client to expect html
      std::cout << http_responses::makeHeader("text", "html");

      std::string SessionID;
      qsp::extractSessionID2(QueryString, SessionID);

      LogFile << Timestamp << " ";
      LogFile << PeerAddress << " " << ReqMethod << " ";
      LogFile << checkVerificationStatus(SessionID) << " ";
      LogFile << QueryString << "\n";

      std::cout << http_responses::makeQueryStringAppended();

      LogFile.close();
    }
    else {
      // Inform client to expect html
      std::cout << http_responses::makeHeader("text", "html");
      std::cout << http_responses::makeFileOpeningError();
    }
    // Reset data for the next request
    Content = "NULL";
    Timestamp = "NULL";
    PeerAddress = "NULL";
    ReqMethod = "NULL";
    QueryString = "NULL";
    https_on = false;
  }

  std::cin.rdbuf(cin_streambuf);
  std::cout.rdbuf(cout_streambuf);
  std::cerr.rdbuf(cerr_streambuf);

  return 0;
}

