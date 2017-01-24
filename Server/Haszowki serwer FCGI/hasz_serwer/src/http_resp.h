#ifndef HTTP_RESP_H
#define HTTP_RESP_H

//C++ libs
#include <sstream>
#include <string>

namespace http_responses
{

std::string makeHeader(const std::string _Category, const std::string _MediaType)
{
  std::stringstream SStr;

  SStr << "Content-type: " << _Category << "/" << _MediaType << "\r\n"
       << "\r\n";

  return SStr.str();
}

std::string makeFileOpeningError()
{
  std::stringstream SStr;

  SStr << "<html>\n"
       << "  <head>\n"
       << "    <title>Error!</title>\n"
       << "  </head>\n"
       << "  <body>\n"
       << "    <h1>Could not open log file!</h1>\n"
       << "  </body>\n"
       << "</html>\n";

  return SStr.str();
}

std::string makeQueryStringAppended()
{
  std::stringstream SStr;

  SStr << "<html>\n"
       << "  <head>\n"
       << "    <title>Success!</title>\n"
       << "  </head>\n"
       << "  <body>\n"
       << "    <h1>Odpowiedz przyjeta/test answer received.</h1>\n"
       << "  </body>\n"
       << "</html>\n";

  return SStr.str();
}

std::string makeNonce(const std::string _Nonce)
{
  std::stringstream SStr;

  SStr << "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
       << "<configuration>\n"
       << "  <nonce bytes=\"" << _Nonce << "\"/>\n"
       << "</configuration>\n";

  return SStr.str();
}

}

#endif
