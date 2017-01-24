#ifndef QSP_H
#define QSP_H

//C++ libs
#include <string>
#include <regex>

namespace qsp
{

bool extractValueOfKey(const std::string& _Key, const std::string& _QueryString, std::string& _Match)
{
  std::string Key = _Key + "(\\w+)&?";
  std::regex Rgx(Key);
  std::smatch Match;

  if (std::regex_search(_QueryString, Match, Rgx)) {
    _Match = Match[1];
    return true;
  }
  else {
    _Match = "NULL";
    return false;
  }
}

bool extractStudentNo(const std::string& _QueryString, std::string& _Match)
{
  std::regex Rgx("student_no=(\\w+)&?");
  std::smatch Match;

  if (std::regex_search(_QueryString, Match, Rgx)) {
    _Match = Match[1];
    return true;
  }
  else {
    _Match = "NULL";
    return false;
  }
}

bool extractCourse(const std::string& _QueryString, std::string& _Match)
{
  std::regex Rgx("course=(\\w+)&?");
  std::smatch Match;

  if (std::regex_search(_QueryString, Match, Rgx)) {
    _Match = Match[1];
    return true;
  }
  else {
    _Match = "NULL";
    return false;
  }
}


bool extractTestID(const std::string _QueryString, std::string& _Match)
{
  std::regex Rgx("test_id=(\\w+)&?");
  std::smatch Match;

  if (std::regex_search(_QueryString, Match, Rgx)) {
    _Match = Match[1];
    return true;
  }
  else {
    _Match = "NULL";
    return false;
  }
}

bool extractHallRow(const std::string _QueryString, std::string& _Match)
{
  std::regex Rgx("hall_row=(\\w+)&?");
  std::smatch Match;

  if (std::regex_search(_QueryString, Match, Rgx)) {
    _Match = Match[1];
    return true;
  }
  else {
    _Match = "NULL";
    return false;
  }
}

bool extractHallSeat(const std::string _QueryString, std::string& _Match)
{
  std::regex Rgx("hall_seat=(\\w+)&?");
  std::smatch Match;

  if (std::regex_search(_QueryString, Match, Rgx)) {
    _Match = Match[1];
    return true;
  }
  else {
    _Match = "NULL";
    return false;
  }
}

bool extractGroup(const std::string _QueryString, std::string& _Match)
{
  std::regex Rgx("group=(\\w+)&?");
  std::smatch Match;

  if (std::regex_search(_QueryString, Match, Rgx)) {
    _Match = Match[1];
    return true;
  }
  else {
    _Match = "NULL";
    return false;
  }
}

bool extractTimestamp(const std::string _QueryString, std::string& _Match)
{
  std::regex Rgx("timestamp=(\\w+)&?");
  std::smatch Match;

  if (std::regex_search(_QueryString, Match, Rgx)) {
    _Match = Match[1];
    return true;
  }
  else {
    _Match = "NULL";
    return false;
  }
}

bool extractQuestionNo(const std::string _QueryString, std::string& _Match)
{
  std::regex Rgx("question_no=(\\w+)&?");
  std::smatch Match;

  if (std::regex_search(_QueryString, Match, Rgx)) {
    _Match = Match[1];
    return true;
  }
  else {
    _Match = "NULL";
    return false;
  }
}

bool extractAnswer(const std::string _QueryString, std::string& _Match)
{
  std::regex Rgx("answer=(\\w+)&?");
  std::smatch Match;

  if (std::regex_search(_QueryString, Match, Rgx)) {
    _Match = Match[1];
    return true;
  }
  else {
    _Match = "NULL";
    return false;
  }
}

bool extractVector(const std::string _QueryString, std::string& _Match)
{
  std::regex Rgx("vector=(\\w+)&?");
  std::smatch Match;

  if (std::regex_search(_QueryString, Match, Rgx)) {
    _Match = Match[1];
    return true;
  }
  else {
    _Match = "NULL";
    return false;
  }
}

bool extractSessionID(const std::string _QueryString, std::string& _Match)
{
  std::regex Rgx("session_id=(\\w+)&?");
  std::smatch Match;

  if (std::regex_search(_QueryString, Match, Rgx)) {
    _Match = Match[1];
    return true;
  }
  else {
    _Match = "NULL";
    return false;
  }
}

bool extractName(const std::string _QueryString, std::string& _Match)
{
  std::regex Rgx("name=(\\w+)&?");
  std::smatch Match;

  if (std::regex_search(_QueryString, Match, Rgx)) {
    _Match = Match[1];
    return true;
  }
  else {
    _Match = "NULL";
    return false;
  }
}

bool extractSurname(const std::string _QueryString, std::string& _Match)
{
  std::regex Rgx("surname=(\\w+)&?");
  std::smatch Match;

  if (std::regex_search(_QueryString, Match, Rgx)) {
    _Match = Match[1];
    return true;
  }
  else {
    _Match = "NULL";
    return false;
  }
}

bool extractSessionID2(const std::string _QueryString, std::string& _Match)
{
  std::regex Rgx("session_id2=(\\w+)&?");
  std::smatch Match;

  if (std::regex_search(_QueryString, Match, Rgx)) {
    _Match = Match[1];
    return true;
  }
  else {
    _Match = "NULL";
    return false;
  }
}

bool extractRequestID(const std::string _QueryString, std::string& _Match)
{
  std::regex Rgx("request_id=(\\w+)&?");
  std::smatch Match;

  if (std::regex_search(_QueryString, Match, Rgx)) {
    _Match = Match[1];
    return true;
  }
  else {
    _Match = "NULL";
    return false;
  }
}

bool extractNonce(const std::string _QueryString, std::string& _Match)
{
  std::regex Rgx("nonce=(\\w+)&?");
  std::smatch Match;

  if (std::regex_search(_QueryString, Match, Rgx)) {
    _Match = Match[1];
    return true;
  }
  else {
    _Match = "NULL";
    return false;
  }
}

}

#endif
