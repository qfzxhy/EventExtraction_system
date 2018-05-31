import tensorflow as tf
from tensorflow.python.ops.rnn_cell_impl import BasicLSTMCell
from tensorflow.python.ops.rnn_cell_impl import RNNCell
from tensorflow.python.ops import math_ops
from tensorflow.python.ops import array_ops
from tensorflow.python.ops.rnn_cell_impl import _linear
from tensorflow.python.ops.rnn_cell_impl import LSTMStateTuple
from tensorflow.python.platform import tf_logging as logging
from tensorflow.python.ops import variable_scope as vs
class MyLSTMCell(RNNCell):

    def __init__(self, num_units, forget_bias=1.0,
                 state_is_tuple=True, activation=None, reuse=None, initializer = None):
        """Initialize the basic LSTM cell.

        Args:
          num_units: int, The number of units in the LSTM cell.
          forget_bias: float, The bias added to forget gates (see above).
          state_is_tuple: If True, accepted and returned states are 2-tuples of
            the `c_state` and `m_state`.  If False, they are concatenated
            along the column axis.  The latter behavior will soon be deprecated.
          activation: Activation function of the inner states.  Default: `tanh`.
          reuse: (optional) Python boolean describing whether to reuse variables
            in an existing scope.  If not `True`, and the existing scope already has
            the given variables, an error is raised.
        """
        super(MyLSTMCell, self).__init__(_reuse=reuse)
        if not state_is_tuple:
            logging.warn("%s: Using a concatenated state is slower and will soon be "
                         "deprecated.  Use state_is_tuple=True.", self)
        self._num_units = num_units
        self._forget_bias = forget_bias
        self._state_is_tuple = state_is_tuple
        self._activation = activation or math_ops.tanh
        self._initializer = initializer

    @property
    def state_size(self):
        return (LSTMStateTuple(self._num_units, self._num_units)
        if self._state_is_tuple else 2 * self._num_units)

    @property
    def output_size(self):
        return self._num_units


    def call(self, inputs, state):
        """Long short-term memory cell (LSTM)."""
        sigmoid = math_ops.sigmoid
        # Parameters of gates are concatenated into one multiply for efficiency.
        main_inputs, aux_inputs = array_ops.split(value=inputs,num_or_size_splits=2,axis=1)
        if self._state_is_tuple:
            c, h = state
        else:
            c, h = array_ops.split(value=state, num_or_size_splits=2, axis=1)

        concat = _linear([main_inputs, h, aux_inputs], 4 * self._num_units, True,kernel_initializer=self._initializer)



        # i = input_gate, f = forget_gate, o = output_gate
        i, f, o, o_a = array_ops.split(value=concat, num_or_size_splits=4, axis=1)

        # j = new_inputa
        with vs.variable_scope('new_inputs'):
            j = _linear([main_inputs,h],self._num_units,True,kernel_initializer=self._initializer)
        with vs.variable_scope('aux_infos'):
            aux_infos = _linear([aux_inputs],self._num_units,False,kernel_initializer=self._initializer)

        new_c = (
                c * sigmoid(f + self._forget_bias) + sigmoid(i) * self._activation(j))
        new_h = self._activation(new_c) * sigmoid(o)

        if self._state_is_tuple:
            new_state = LSTMStateTuple(new_c, new_h)
        else:
            new_state = array_ops.concat([new_c, new_h], 1)
        return new_h, new_state
