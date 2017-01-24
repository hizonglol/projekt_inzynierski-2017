#ifndef TIMESTAMP_H
#define TIMESTAMP_H

//C++ libs
#include <string>
#include <iomanip>
#include <chrono>
#include <sstream>
//Headers
#include "date.h"

std::string makeTimestamp()
{
  std::stringstream SStr;

  namespace chr = std::chrono;
  namespace dt = date;

  auto TimePoint = chr::high_resolution_clock::now();
  auto SysDays = dt::floor<dt::days>(TimePoint);
  auto YMD = dt::year_month_day{SysDays};
  auto Time = dt::make_time(chr::duration_cast<chr::milliseconds>(TimePoint-SysDays));

  SStr << "["
       << std::setfill('0') << std::setw(2)
       << YMD.day() << "/" << YMD.month() << "/" << YMD.year() << ":"
       << std::setfill('0') << std::setw(2) << Time.hours().count() << ":"
       << std::setfill('0') << std::setw(2) << Time.minutes().count() << ":"
       << std::setfill('0') << std::setw(2) << Time.seconds().count() << "."
       << std::setfill('0') << std::setw(3) << Time.subseconds().count()
       << "]";

  return SStr.str();
}

#endif
