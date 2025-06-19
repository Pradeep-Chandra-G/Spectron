import React, { useState, useEffect, useRef } from 'react';
import { Upload, Send, File, Trash2, MessageCircle, Bot, User, Loader, CheckCircle, XCircle, Clock, FileText, AlertCircle, Menu, X } from 'lucide-react';

const RagChatApp = () => {
  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState('');
  const [documents, setDocuments] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [dragActive, setDragActive] = useState(false);
  const [activeTab, setActiveTab] = useState('chat');
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const messagesEndRef = useRef(null);
  const fileInputRef = useRef(null);

  const API_BASE = 'http://localhost:8080/api';

  useEffect(() => {
    fetchDocuments();
    fetchChatHistory();
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const fetchDocuments = async () => {
    try {
      const response = await fetch(`${API_BASE}/documents`);
      if (response.ok) {
        const docs = await response.json();
        setDocuments(docs);
      }
    } catch (error) {
      console.error('Error fetching documents:', error);
    }
  };

  const fetchChatHistory = async () => {
    try {
      const response = await fetch(`${API_BASE}/chat/history`);
      if (response.ok) {
        const history = await response.json();
        const formattedMessages = history.reverse().flatMap(msg => [
          { id: `q-${msg.id}`, type: 'user', content: msg.question, timestamp: msg.timestamp },
          { id: `a-${msg.id}`, type: 'assistant', content: msg.answer, timestamp: msg.timestamp }
        ]);
        setMessages(formattedMessages);
      }
    } catch (error) {
      console.error('Error fetching chat history:', error);
    }
  };

  const handleSendMessage = async () => {
    if (!inputMessage.trim() || isLoading) return;

    const userMessage = {
      id: Date.now(),
      type: 'user',
      content: inputMessage,
      timestamp: new Date().toISOString()
    };

    setMessages(prev => [...prev, userMessage]);
    setInputMessage('');
    setIsLoading(true);

    try {
      const response = await fetch(`${API_BASE}/chat`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ question: inputMessage }),
      });

      if (response.ok) {
        const data = await response.json();
        const assistantMessage = {
          id: Date.now() + 1,
          type: 'assistant',
          content: data.answer,
          timestamp: new Date().toISOString()
        };
        setMessages(prev => [...prev, assistantMessage]);
      } else {
        throw new Error('Failed to get response');
      }
    } catch (error) {
      const errorMessage = {
        id: Date.now() + 1,
        type: 'assistant',
        content: 'Sorry, I encountered an error processing your request.',
        timestamp: new Date().toISOString()
      };
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleFileUpload = async (files) => {
    const formData = new FormData();
    Array.from(files).forEach(file => {
      formData.append('file', file);
    });

    try {
      const response = await fetch(`${API_BASE}/documents/upload`, {
        method: 'POST',
        body: formData,
      });

      if (response.ok) {
        fetchDocuments();
      } else {
        alert('Failed to upload file');
      }
    } catch (error) {
      console.error('Error uploading file:', error);
      alert('Error uploading file');
    }
  };

  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileUpload(e.dataTransfer.files);
    }
  };

  const deleteDocument = async (id) => {
    try {
      const response = await fetch(`${API_BASE}/documents/${id}`, {
        method: 'DELETE',
      });
      if (response.ok) {
        fetchDocuments();
      }
    } catch (error) {
      console.error('Error deleting document:', error);
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'COMPLETED':
        return <CheckCircle className="w-4 h-4 text-green-500" />;
      case 'FAILED':
        return <XCircle className="w-4 h-4 text-red-500" />;
      case 'PROCESSING':
        return <Loader className="w-4 h-4 text-blue-500 animate-spin" />;
      default:
        return <Clock className="w-4 h-4 text-yellow-500" />;
    }
  };

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900">
        <div className="container mx-auto px-2 sm:px-4 py-3 sm:py-6">
          {/* Header */}
          <div className="mb-4 sm:mb-8">
            <div className="flex items-center justify-between">
              <div>
                <h1 className="text-2xl sm:text-3xl lg:text-4xl font-bold text-white mb-1 sm:mb-2 bg-gradient-to-r from-purple-400 to-pink-400 bg-clip-text text-transparent">
                  Spectron
                </h1>
                <p className="text-sm sm:text-base text-slate-300">Chat with your documents using Mistral 7B</p>
              </div>
              {/* Mobile menu button */}
              <button
                  onClick={() => setSidebarOpen(!sidebarOpen)}
                  className="lg:hidden p-2 text-white bg-slate-700/50 rounded-lg border border-slate-600/50"
              >
                {sidebarOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
              </button>
            </div>
          </div>

          <div className="flex gap-2 sm:gap-6 h-[calc(100vh-8rem)] sm:h-[calc(100vh-12rem)]">
            {/* Mobile Sidebar Overlay */}
            {sidebarOpen && (
                <div
                    className="lg:hidden fixed inset-0 bg-black/50 backdrop-blur-sm z-40"
                    onClick={() => setSidebarOpen(false)}
                />
            )}

            {/* Sidebar */}
            <div className={`
            fixed lg:relative top-0 left-0 z-50 lg:z-auto
            w-80 sm:w-80 lg:w-80 xl:w-96
            h-full lg:h-auto
            bg-slate-800/50 backdrop-blur-sm rounded-none lg:rounded-2xl 
            border-r lg:border border-slate-700/50 
            overflow-hidden
            transform transition-transform duration-300 ease-in-out
            ${sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
          `}>
              <div className="p-3 sm:p-4 border-b border-slate-700/50">
                <div className="flex items-center justify-between mb-3 lg:mb-0">
                  <h2 className="text-white font-semibold lg:hidden">Menu</h2>
                  <button
                      onClick={() => setSidebarOpen(false)}
                      className="lg:hidden p-1 text-slate-400 hover:text-white"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>
                <div className="flex space-x-1 bg-slate-700/30 p-1 rounded-lg">
                  <button
                      onClick={() => setActiveTab('chat')}
                      className={`flex-1 px-2 sm:px-3 py-2 rounded-md text-xs sm:text-sm font-medium transition-all ${
                          activeTab === 'chat'
                              ? 'bg-purple-600 text-white shadow-lg'
                              : 'text-slate-300 hover:text-white hover:bg-slate-700/50'
                      }`}
                  >
                    <MessageCircle className="w-3 h-3 sm:w-4 sm:h-4 inline mr-1 sm:mr-2" />
                    Chat
                  </button>
                  <button
                      onClick={() => setActiveTab('documents')}
                      className={`flex-1 px-2 sm:px-3 py-2 rounded-md text-xs sm:text-sm font-medium transition-all ${
                          activeTab === 'documents'
                              ? 'bg-purple-600 text-white shadow-lg'
                              : 'text-slate-300 hover:text-white hover:bg-slate-700/50'
                      }`}
                  >
                    <File className="w-3 h-3 sm:w-4 sm:h-4 inline mr-1 sm:mr-2" />
                    Documents
                  </button>
                </div>
              </div>

              <div className="flex-1 overflow-y-auto">
                {activeTab === 'documents' && (
                    <div className="p-3 sm:p-4">
                      <div
                          className={`border-2 border-dashed rounded-lg p-4 sm:p-6 text-center transition-all ${
                              dragActive
                                  ? 'border-purple-400 bg-purple-400/10'
                                  : 'border-slate-600 hover:border-slate-500'
                          }`}
                          onDragEnter={handleDrag}
                          onDragLeave={handleDrag}
                          onDragOver={handleDrag}
                          onDrop={handleDrop}
                      >
                        <Upload className="w-6 h-6 sm:w-8 sm:h-8 text-slate-400 mx-auto mb-2" />
                        <p className="text-slate-300 text-xs sm:text-sm mb-2">
                          Drag & drop files here or click to upload
                        </p>
                        <button
                            onClick={() => fileInputRef.current?.click()}
                            className="px-3 sm:px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg text-xs sm:text-sm transition-colors"
                        >
                          Choose Files
                        </button>
                        <input
                            ref={fileInputRef}
                            type="file"
                            multiple
                            accept=".pdf,.doc,.docx,.txt,.md"
                            onChange={(e) => handleFileUpload(e.target.files)}
                            className="hidden"
                        />
                      </div>

                      <div className="mt-4 space-y-2 max-h-64 sm:max-h-96 overflow-y-auto">
                        {documents.map((doc) => (
                            <div
                                key={doc.id}
                                className="bg-slate-700/30 rounded-lg p-2 sm:p-3 border border-slate-600/30"
                            >
                              <div className="flex items-center justify-between">
                                <div className="flex items-center space-x-2 flex-1 min-w-0">
                                  <FileText className="w-3 h-3 sm:w-4 sm:h-4 text-slate-400 flex-shrink-0" />
                                  <div className="min-w-0 flex-1">
                                    <p className="text-slate-200 text-xs sm:text-sm font-medium truncate">
                                      {doc.originalName}
                                    </p>
                                    <p className="text-slate-400 text-xs">
                                      {formatFileSize(doc.fileSize)}
                                    </p>
                                  </div>
                                </div>
                                <div className="flex items-center space-x-2">
                                  {getStatusIcon(doc.status)}
                                  <button
                                      onClick={() => deleteDocument(doc.id)}
                                      className="p-1 text-slate-400 hover:text-red-400 transition-colors"
                                  >
                                    <Trash2 className="w-3 h-3 sm:w-4 sm:h-4" />
                                  </button>
                                </div>
                              </div>
                              {doc.status === 'COMPLETED' && doc.chunkCount && (
                                  <p className="text-slate-400 text-xs mt-1">
                                    {doc.chunkCount} chunks processed
                                  </p>
                              )}
                              {doc.status === 'FAILED' && doc.errorMessage && (
                                  <p className="text-red-400 text-xs mt-1 flex items-center">
                                    <AlertCircle className="w-3 h-3 mr-1" />
                                    {doc.errorMessage}
                                  </p>
                              )}
                            </div>
                        ))}
                      </div>
                    </div>
                )}

                {activeTab === 'chat' && (
                    <div className="p-3 sm:p-4">
                      <div className="text-slate-300 text-xs sm:text-sm">
                        <p className="mb-2">💡 Tips:</p>
                        <ul className="space-y-1 text-xs text-slate-400">
                          <li>• Upload documents first</li>
                          <li>• Ask specific questions</li>
                          <li>• Reference document content</li>
                        </ul>
                      </div>
                    </div>
                )}
              </div>
            </div>

            {/* Main Chat Area */}
            <div className="flex-1 bg-slate-800/50 backdrop-blur-sm rounded-lg sm:rounded-2xl border border-slate-700/50 flex flex-col overflow-hidden">
              {/* Chat Messages */}
              <div className="flex-1 overflow-y-auto p-3 sm:p-4 lg:p-6 space-y-3 sm:space-y-4 lg:space-y-6">
                {messages.length === 0 ? (
                    <div className="text-center text-slate-400 mt-10 sm:mt-20">
                      <Bot className="w-12 h-12 sm:w-16 sm:h-16 mx-auto mb-4 text-slate-500" />
                      <p className="text-base sm:text-lg mb-2">Welcome to Spectron</p>
                      <p className="text-xs sm:text-sm">Upload documents and start asking questions!</p>
                    </div>
                ) : (
                    messages.map((message) => (
                        <div
                            key={message.id}
                            className={`flex ${message.type === 'user' ? 'justify-end' : 'justify-start'}`}
                        >
                          <div
                              className={`max-w-[85%] sm:max-w-[80%] lg:max-w-[70%] rounded-xl sm:rounded-2xl px-3 sm:px-4 py-2 sm:py-3 ${
                                  message.type === 'user'
                                      ? 'bg-gradient-to-r from-purple-600 to-pink-600 text-white'
                                      : 'bg-slate-700/50 border border-slate-600/50 text-slate-100'
                              }`}
                          >
                            <div className="flex items-start space-x-2">
                              {message.type === 'assistant' && (
                                  <Bot className="w-4 h-4 sm:w-5 sm:h-5 text-purple-400 flex-shrink-0 mt-0.5" />
                              )}
                              {message.type === 'user' && (
                                  <User className="w-4 h-4 sm:w-5 sm:h-5 text-white flex-shrink-0 mt-0.5" />
                              )}
                              <div className="flex-1">
                                <p className="text-xs sm:text-sm leading-relaxed whitespace-pre-wrap">
                                  {message.content}
                                </p>
                              </div>
                            </div>
                          </div>
                        </div>
                    ))
                )}

                {isLoading && (
                    <div className="flex justify-start">
                      <div className="bg-slate-700/50 border border-slate-600/50 rounded-xl sm:rounded-2xl px-3 sm:px-4 py-2 sm:py-3">
                        <div className="flex items-center space-x-2">
                          <Bot className="w-4 h-4 sm:w-5 sm:h-5 text-purple-400" />
                          <div className="flex space-x-1">
                            <div className="w-2 h-2 bg-slate-400 rounded-full animate-bounce"></div>
                            <div className="w-2 h-2 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '0.1s' }}></div>
                            <div className="w-2 h-2 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }}></div>
                          </div>
                        </div>
                      </div>
                    </div>
                )}

                <div ref={messagesEndRef} />
              </div>

              {/* Chat Input */}
              <div className="p-3 sm:p-4 lg:p-6 border-t border-slate-700/50">
                <div className="flex flex-col sm:flex-row space-y-2 sm:space-y-0 sm:space-x-4">
                  <div className="flex-1 relative">
                    <input
                        type="text"
                        value={inputMessage}
                        onChange={(e) => setInputMessage(e.target.value)}
                        onKeyPress={(e) => e.key === 'Enter' && handleSendMessage()}
                        placeholder="Ask a question about your documents..."
                        className="w-full px-3 sm:px-4 py-2 sm:py-3 bg-slate-700/50 border border-slate-600/50 rounded-lg sm:rounded-xl text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all text-sm sm:text-base"
                        disabled={isLoading}
                    />
                  </div>
                  <button
                      onClick={handleSendMessage}
                      disabled={!inputMessage.trim() || isLoading}
                      className="px-4 sm:px-6 py-2 sm:py-3 bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-700 hover:to-pink-700 disabled:from-slate-600 disabled:to-slate-600 text-white rounded-lg sm:rounded-xl transition-all disabled:cursor-not-allowed flex items-center justify-center space-x-2 text-sm sm:text-base"
                  >
                    {isLoading ? (
                        <Loader className="w-4 h-4 sm:w-5 sm:h-5 animate-spin" />
                    ) : (
                        <Send className="w-4 h-4 sm:w-5 sm:h-5" />
                    )}
                    <span className="hidden sm:inline">Send</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
  );
};

export default RagChatApp;