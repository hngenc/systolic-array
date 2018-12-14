A Systolic(ish) Hardware Description Language
===============

This (as-yet-unnamed) project provides a Scala-embedded DSL which can be used to describe _systolic arrays_, a type of highly-parallel processor. The language helps users to separate the _functional correctness_ of their design from the _dataflow_ which it implements, and enables users to quickly iterate over many different dataflows to find the one which best suits their purposes.

## Introduction
With the end of Moore's Law, CPU performance is no longer increasing at the speed it once did. To run compute-intensive workloads today, computer architects are increasingly turning towards specialized hardware accelerators, especially in cases where there is massive parallelism or locality that traditional CPUs are ill-equipped to exploit. Systolic arrays are one such type of hardware accelerator; they have successfully been used to accelerate critical algorithms such as convolution [Stanford], matrix multiplication [TPU], sorting [SORT] and many more, both in industry and in academia.

Systolic arrays are two-dimensional arrays of small processing units (called _PEs_) carrying out the same function, which, in most cases, communicate only with the PEs directly adjacent to themselves. Thus, they help to lower communication costs, and they shine with workloads that exhibit high locality where data can be shared across many adjacent PEs within the same array.

How exactly that data flows through the array is determined by a mapping known as a _spacetime transform_, which this language helps users to explore. In the following sections, we illustrate by focusing on the example of a systolic array used to compute matrix multiplications.

## Matrix Multiplication

## Functional Notation

## Spacetime Transforms

### Constraints

## Compiler Output

## Mapping Larger Matrices

## Conclusion

## References

[Stanford] Yang, Xuan, et al. "DNN Dataflow Choice Is Overrated." _arXiv preprint arXiv:1809.04070_ (2018).

[TPU] Jouppi, Norman P., et al. "In-datacenter performance analysis of a tensor processing unit." _Computer Architecture (ISCA), 2017 ACM/IEEE 44th Annual International Symposium on._ IEEE, 2017.

[SORT]: Schwiegelshohn, Uwe. "A shortperiodic two-dimensional systolic sorting algorithm." _Systolic Arrays, 1988., Proceedings of the International Conference on._ IEEE, 1988.
