// VERSION 1.0.5

//C++ libs
#include <fstream>
#include <iostream>
#include <string>
#include <thread>
//Other libraries
#include "fcgio.h"
#include <curl/curl.h>
//Headers
#include "base64.h"
#include "tools.h"
#include "http_resp.h"
#include "qsp.h"
#include "timestamp.h"

int main(void)
{
  // Initializing FCGI
  FCGX_Init();
  // FCGI request class
  FCGX_Request* pRequest = newRequest();

  while (true) {
    if (FCGX_Accept_r(pRequest) >= 0) {
      std::thread t1(taskFCGI, pRequest);
      t1.join();
    }
  }

  return 0;
}

