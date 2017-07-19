//
// Created by Udit Mukherjee on 15/06/17.
//

#ifndef CRYFORHELP_TIME_PROFILER_H
#define CRYFORHELP_TIME_PROFILER_H

#include <stack>
#include <time.h>

/**
 * An util class to profile the performance of code.
 * Usage: Init profiler, profiler.start(), double interval = profiler.stopAndGetInterval();
 */
class Profiler {
private:
    std::stack<timespec> mTimespecs;

public:
    Profiler();

    ~Profiler();

    /**
     * Start profiling. The profiler put a monotonic time-stamp to the stack.
     */
    void start();

    /**
     * Stop profiling and pop the time-stamp, then return the interval in
     * milliseconds.
     */
    double stopAndGetInterval();
};

#endif //CRYFORHELP_TIME_PROFILER_H

